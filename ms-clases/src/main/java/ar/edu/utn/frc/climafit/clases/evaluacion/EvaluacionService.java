package ar.edu.utn.frc.climafit.clases.evaluacion;

import ar.edu.utn.frc.climafit.clases.aviso.AvisoService;
import ar.edu.utn.frc.climafit.clases.aviso.TipoEvento;
import ar.edu.utn.frc.climafit.clases.clase.Clase;
import ar.edu.utn.frc.climafit.clases.client.actividades.ActividadDto;
import ar.edu.utn.frc.climafit.clases.client.actividades.ActividadesClient;
import ar.edu.utn.frc.climafit.clases.client.actividades.EspacioDto;
import ar.edu.utn.frc.climafit.clases.client.clima.ClimaClient;
import ar.edu.utn.frc.climafit.clases.client.clima.EvaluacionClimaDto;
import ar.edu.utn.frc.climafit.clases.client.clima.VentanaDto;
import ar.edu.utn.frc.climafit.clases.estado.CambioEstado;
import ar.edu.utn.frc.climafit.clases.estado.CambioEstadoService;
import ar.edu.utn.frc.climafit.clases.estado.EstadoClase;
import ar.edu.utn.frc.climafit.clases.reprogramacion.PlanReprogramacion;
import ar.edu.utn.frc.climafit.clases.reprogramacion.ReprogramacionService;
import ar.edu.utn.frc.climafit.clases.reprogramacion.ResultadoReprogramacion;
import ar.edu.utn.frc.climafit.commons.error.ServicioNoDisponibleException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tarea 4.6 - evaluación climática de una clase (pasos P1 a P7 de la propuesta, 4.3).
 *
 * <p>Orden de trabajo: primero se CONSULTA todo (actividad, espacio, clima y, si hace falta, el
 * nuevo día) y recién después se CAMBIA el estado y se avisa. Si cualquier consulta falla,
 * la evaluación queda FALLIDA y la clase no cambia (RN16).
 */
@Service
public class EvaluacionService {

    /** RN09: estados evaluables. */
    private static final Set<EstadoClase> ESTADOS_EVALUABLES =
            EnumSet.of(EstadoClase.PROGRAMADA, EstadoClase.REPROGRAMADA, EstadoClase.EN_RIESGO);

    /** RN09: la clase tiene que empezar dentro de las próximas 24 horas. */
    static final int HORAS_DE_ANTICIPACION = 24;

    private final ActividadesClient actividades;
    private final ClimaClient clima;
    private final EvaluacionClimaticaRepository evaluaciones;
    private final CambioEstadoService cambioEstado;
    private final AvisoService aviso;
    private final ReprogramacionService reprogramacion;
    private final Clock reloj;

    public EvaluacionService(ActividadesClient actividades, ClimaClient clima,
                             EvaluacionClimaticaRepository evaluaciones, CambioEstadoService cambioEstado,
                             AvisoService aviso, ReprogramacionService reprogramacion, Clock reloj) {
        this.actividades = actividades;
        this.clima = clima;
        this.evaluaciones = evaluaciones;
        this.cambioEstado = cambioEstado;
        this.aviso = aviso;
        this.reprogramacion = reprogramacion;
        this.reloj = reloj;
    }

    public static Set<EstadoClase> estadosEvaluables() {
        return ESTADOS_EVALUABLES;
    }

    /** RN09: clase al aire libre, en un estado evaluable, que empieza dentro de las próximas 24 h. */
    public boolean esEvaluable(Clase clase) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return clase.isAlAireLibre()
                && ESTADOS_EVALUABLES.contains(clase.getEstado())
                && !clase.getInicio().isBefore(ahora)
                && !clase.getInicio().isAfter(ahora.plusHours(HORAS_DE_ANTICIPACION));
    }

    /** Evaluación manual (profesor o administrador). RN09: si no cumple, 409 CLASE_NO_EVALUABLE. */
    @Transactional
    public EvaluacionRespuesta evaluarManual(Clase clase, String responsable) {
        if (!esEvaluable(clase)) {
            throw new ClaseNoEvaluableException("La clase " + clase.getId() + " no se puede evaluar: tiene que ser "
                    + "al aire libre, estar PROGRAMADA, REPROGRAMADA o EN_RIESGO y empezar dentro de las próximas "
                    + HORAS_DE_ANTICIPACION + " horas");
        }
        return evaluar(clase, responsable);
    }

    /**
     * Evalúa una clase que ya cumple RN09 (la tarea programada las selecciona con la consulta
     * de P1) y aplica la decisión de RN11.
     *
     * @param responsable sub del usuario que pidió la evaluación, o SISTEMA
     */
    @Transactional
    public EvaluacionRespuesta evaluar(Clase clase, String responsable) {
        LocalDateTime inicio = clase.getInicio();

        // P2 y P3: criterios de la actividad, coordenadas del espacio y evaluación de la ventana.
        ActividadDto actividad;
        EvaluacionClimaDto evaluacion;
        EspacioDto espacio;
        try {
            actividad = actividades.obtenerActividad(clase.getTipoActividadId());
            espacio = actividades.obtenerEspacio(clase.getEspacioId());
            evaluacion = clima.evaluar(espacio.latitud(), espacio.longitud(),
                    ReprogramacionService.criterios(actividad),
                    List.of(new VentanaDto(inicio, clase.getFin()))).get(0);
        } catch (ServicioNoDisponibleException e) {
            return fallida(clase, "No se pudo evaluar el clima: " + e.getMessage());   // RN16
        }

        // P4 - RN11: decisión según el resultado.
        switch (evaluacion.resultado()) {
            case NO_APTA -> {
                // P5: buscar el nuevo día ANTES de suspender (si ms-clima falla, la clase no cambia).
                PlanReprogramacion plan;
                try {
                    plan = reprogramacion.buscarNuevoDia(clase, actividad, espacio);
                } catch (ServicioNoDisponibleException e) {
                    return fallida(clase, "Evaluación NO_APTA (" + evaluacion.motivo()
                            + "), pero no se pudo buscar un nuevo día: " + e.getMessage());   // RN16
                }
                guardar(clase, evaluacion);
                // P5, P6 y P7: suspender, reprogramar (o dejar sin fecha) y avisar.
                ResultadoReprogramacion r = reprogramacion.suspenderYReprogramar(
                        clase, actividad, plan, evaluacion.motivo(), responsable);
                return respuesta(evaluacion, clase, r.nuevoInicio());
            }
            case EN_RIESGO -> {
                guardar(clase, evaluacion);
                // Si ya estaba EN_RIESGO no se vuelve a cambiar ni a alertar: la propuesta dice que
                // la clase sigue vigente y la próxima evaluación la analiza de nuevo (RN12).
                if (clase.getEstado() != EstadoClase.EN_RIESGO) {
                    CambioEstado cambio = cambioEstado.cambiarEstado(clase, EstadoClase.EN_RIESGO,
                            evaluacion.motivo(), responsable);
                    aviso.avisar(cambio, TipoEvento.ALERTA_RIESGO,
                            MensajesClima.alertaRiesgo(actividad.nombre(), clase, evaluacion.motivo()),
                            List.of(clase.getProfesorId()));
                }
                return respuesta(evaluacion, clase, null);
            }
            default -> {   // APTA: no cambia nada
                guardar(clase, evaluacion);
                return respuesta(evaluacion, clase, null);
            }
        }
    }

    private void guardar(Clase clase, EvaluacionClimaDto e) {
        evaluaciones.save(new EvaluacionClimatica(clase.getId(), LocalDateTime.now(reloj), clase.getInicio(),
                e.resultado(), e.motivo(), e.probLluvia(), e.vientoKmh(), e.temperaturaC(), e.tormenta(),
                e.precipitacionMm()));
    }

    /** RN16: se registra la evaluación FALLIDA, la clase no cambia y se reintenta en la próxima ejecución. */
    private EvaluacionRespuesta fallida(Clase clase, String motivo) {
        evaluaciones.save(new EvaluacionClimatica(clase.getId(), LocalDateTime.now(reloj), clase.getInicio(),
                ResultadoEvaluacion.FALLIDA, motivo, null, null, null, null, null));
        return new EvaluacionRespuesta(ResultadoEvaluacion.FALLIDA, motivo, null, null, null, null, null,
                clase.getEstado(), null);
    }

    private static EvaluacionRespuesta respuesta(EvaluacionClimaDto e, Clase clase, LocalDateTime nuevoInicio) {
        return new EvaluacionRespuesta(e.resultado(), e.motivo(), e.probLluvia(), e.vientoKmh(),
                e.temperaturaC(), e.tormenta(), e.precipitacionMm(), clase.getEstado(), nuevoInicio);
    }
}
