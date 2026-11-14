package ar.edu.utn.frc.climafit.clases.reprogramacion;

import ar.edu.utn.frc.climafit.clases.aviso.AvisoService;
import ar.edu.utn.frc.climafit.clases.aviso.TipoEvento;
import ar.edu.utn.frc.climafit.clases.clase.Clase;
import ar.edu.utn.frc.climafit.clases.clase.ClaseRepository;
import ar.edu.utn.frc.climafit.clases.client.actividades.ActividadDto;
import ar.edu.utn.frc.climafit.clases.client.actividades.EspacioDto;
import ar.edu.utn.frc.climafit.clases.client.clima.ClimaClient;
import ar.edu.utn.frc.climafit.clases.client.clima.CriteriosClimaDto;
import ar.edu.utn.frc.climafit.clases.client.clima.EvaluacionClimaDto;
import ar.edu.utn.frc.climafit.clases.client.clima.VentanaDto;
import ar.edu.utn.frc.climafit.clases.estado.CambioEstado;
import ar.edu.utn.frc.climafit.clases.estado.CambioEstadoService;
import ar.edu.utn.frc.climafit.clases.estado.EstadoClase;
import ar.edu.utn.frc.climafit.clases.evaluacion.MensajesClima;
import ar.edu.utn.frc.climafit.clases.evaluacion.ResultadoEvaluacion;
import ar.edu.utn.frc.climafit.clases.inscripcion.EstadoInscripcion;
import ar.edu.utn.frc.climafit.clases.inscripcion.Inscripcion;
import ar.edu.utn.frc.climafit.clases.inscripcion.InscripcionRepository;
import ar.edu.utn.frc.climafit.commons.error.ServicioNoDisponibleException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Tarea 4.7 - reprogramación automática (RN13), en dos tiempos:
 *
 * <ol>
 *   <li>{@link #buscarNuevoDia}: busca el primer día viable. Solo consulta, no cambia nada.
 *       Si ms-clima falla acá, la excepción sube y la clase queda como estaba (RN16).</li>
 *   <li>{@link #suspenderYReprogramar}: recién con el plan en la mano aplica los cambios de
 *       estado y manda los avisos.</li>
 * </ol>
 *
 * <p>Separarlo así evita dejar una clase SUSPENDIDA "a medias" si el clima se cae justo cuando
 * se busca el nuevo día: la clase RN09 solo evalúa PROGRAMADA, REPROGRAMADA y EN_RIESGO, así
 * que una SUSPENDIDA nunca se reintentaría sola.
 */
@Service
public class ReprogramacionService {

    /** RN13: se prueba el mismo horario en cada uno de los 7 días siguientes. */
    static final int DIAS_DE_BUSQUEDA = 7;
    static final String RESPONSABLE_SISTEMA = "SISTEMA";

    private final ClimaClient clima;
    private final ClaseRepository clases;
    private final InscripcionRepository inscripciones;
    private final CambioEstadoService cambioEstado;
    private final AvisoService aviso;

    public ReprogramacionService(ClimaClient clima, ClaseRepository clases,
                                 InscripcionRepository inscripciones, CambioEstadoService cambioEstado,
                                 AvisoService aviso) {
        this.clima = clima;
        this.clases = clases;
        this.inscripciones = inscripciones;
        this.cambioEstado = cambioEstado;
        this.aviso = aviso;
    }

    /**
     * RN13: mismo horario en los 7 días siguientes; descarta los días en que el profesor tiene
     * otra clase (RN04); evalúa los restantes en UNA sola llamada a ms-clima y elige el primero APTA.
     *
     * @throws ServicioNoDisponibleException si ms-clima falla (la clase no se modificó)
     */
    public PlanReprogramacion buscarNuevoDia(Clase clase, ActividadDto actividad, EspacioDto espacio) {
        List<VentanaDto> candidatas = new ArrayList<>();
        for (int dia = 1; dia <= DIAS_DE_BUSQUEDA; dia++) {
            LocalDateTime inicio = clase.getInicio().plusDays(dia);
            LocalDateTime fin = inicio.plusMinutes(clase.getDuracionMin());
            // RN04: el profesor no puede tener dos clases superpuestas.
            if (!clases.existeSuperposicionProfesor(clase.getProfesorId(), inicio, fin, clase.getId())) {
                candidatas.add(new VentanaDto(inicio, fin));
            }
        }
        if (candidatas.isEmpty()) {
            return new PlanReprogramacion(null);
        }

        List<EvaluacionClimaDto> evaluaciones = clima.evaluar(espacio.latitud(), espacio.longitud(),
                criterios(actividad), candidatas);
        if (evaluaciones.size() != candidatas.size()) {
            throw new ServicioNoDisponibleException("ms-clima devolvió una cantidad inesperada de evaluaciones");
        }
        for (int i = 0; i < candidatas.size(); i++) {
            if (evaluaciones.get(i).resultado() == ResultadoEvaluacion.APTA) {
                return new PlanReprogramacion(candidatas.get(i).inicio());
            }
        }
        return new PlanReprogramacion(null);
    }

    /**
     * Pasa la clase a SUSPENDIDA y avisa; después, según el plan, la deja REPROGRAMADA en la
     * nueva fecha (las inscripciones ACTIVA siguen activas, no se tocan) o SUSPENDIDA_SIN_FECHA.
     *
     * @param motivoClimatico lo que decidió la suspensión (por ejemplo, el motivo de la evaluación)
     * @param responsable     quién causó la suspensión: el sub del usuario o SISTEMA
     */
    public ResultadoReprogramacion suspenderYReprogramar(Clase clase, ActividadDto actividad,
                                                         PlanReprogramacion plan, String motivoClimatico,
                                                         String responsable) {
        LocalDateTime inicioOriginal = clase.getInicio();
        List<String> alumnos = alumnosActivos(clase);

        CambioEstado suspension = cambioEstado.cambiarEstado(clase, EstadoClase.SUSPENDIDA, motivoClimatico, responsable);
        aviso.avisar(suspension, TipoEvento.SUSPENSION,
                MensajesClima.suspension(actividad.nombre(), inicioOriginal, motivoClimatico), alumnos);

        if (plan.hayDia()) {
            clase.setInicio(plan.nuevoInicio());
            clases.save(clase);
            CambioEstado reprogramacion = cambioEstado.cambiarEstado(clase, EstadoClase.REPROGRAMADA,
                    "Reprogramada automáticamente: " + motivoClimatico, RESPONSABLE_SISTEMA);
            // cambiarEstado no recibe las fechas: se completan acá (el CambioEstado es una
            // entidad gestionada dentro de la transacción y se guarda al cerrarla).
            reprogramacion.setInicioAnterior(inicioOriginal);
            reprogramacion.setInicioNuevo(plan.nuevoInicio());
            aviso.avisar(reprogramacion, TipoEvento.REPROGRAMACION,
                    MensajesClima.reprogramacion(actividad.nombre(), inicioOriginal, plan.nuevoInicio(),
                            motivoClimatico), alumnos);
            return new ResultadoReprogramacion(EstadoClase.REPROGRAMADA, plan.nuevoInicio());
        }

        CambioEstado sinFecha = cambioEstado.cambiarEstado(clase, EstadoClase.SUSPENDIDA_SIN_FECHA,
                "No hay un día viable en los próximos " + DIAS_DE_BUSQUEDA + " días", RESPONSABLE_SISTEMA);
        sinFecha.setInicioAnterior(inicioOriginal);
        aviso.avisar(sinFecha, TipoEvento.SIN_FECHA,
                MensajesClima.sinFecha(actividad.nombre(), inicioOriginal, motivoClimatico), alumnos);
        return new ResultadoReprogramacion(EstadoClase.SUSPENDIDA_SIN_FECHA, null);
    }

    private List<String> alumnosActivos(Clase clase) {
        return inscripciones.findByClaseIdAndEstado(clase.getId(), EstadoInscripcion.ACTIVA).stream()
                .map(Inscripcion::getAlumnoId).toList();
    }

    public static CriteriosClimaDto criterios(ActividadDto a) {
        return new CriteriosClimaDto(a.probLluviaMax().intValue(), a.vientoMaxKmh().intValue(),
                a.tempMinC().intValue(), a.lluviaMaxMmHora());
    }
}
