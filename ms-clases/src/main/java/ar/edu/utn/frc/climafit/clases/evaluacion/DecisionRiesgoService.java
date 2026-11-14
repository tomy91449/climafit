package ar.edu.utn.frc.climafit.clases.evaluacion;

import ar.edu.utn.frc.climafit.clases.clase.Clase;
import ar.edu.utn.frc.climafit.clases.client.actividades.ActividadDto;
import ar.edu.utn.frc.climafit.clases.client.actividades.ActividadesClient;
import ar.edu.utn.frc.climafit.clases.client.actividades.EspacioDto;
import ar.edu.utn.frc.climafit.clases.estado.CambioEstadoService;
import ar.edu.utn.frc.climafit.clases.estado.EstadoClase;
import ar.edu.utn.frc.climafit.clases.reprogramacion.PlanReprogramacion;
import ar.edu.utn.frc.climafit.clases.reprogramacion.ReprogramacionService;
import ar.edu.utn.frc.climafit.commons.error.ServicioNoDisponibleException;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RN12 - el profesor mantiene o suspende una clase EN_RIESGO. */
@Service
public class DecisionRiesgoService {

    private final CambioEstadoService cambioEstado;
    private final ActividadesClient actividades;
    private final ReprogramacionService reprogramacion;
    private final EvaluacionClimaticaRepository evaluaciones;
    private final Clock reloj;

    public DecisionRiesgoService(CambioEstadoService cambioEstado, ActividadesClient actividades,
                                 ReprogramacionService reprogramacion,
                                 EvaluacionClimaticaRepository evaluaciones, Clock reloj) {
        this.cambioEstado = cambioEstado;
        this.actividades = actividades;
        this.reprogramacion = reprogramacion;
        this.evaluaciones = evaluaciones;
        this.reloj = reloj;
    }

    /**
     * Aplica RN12: MANTENER devuelve la clase a su estado previo; SUSPENDER avisa y ejecuta
     * la reprogramación automática. El controller verifica que el usuario sea el profesor
     * dueño (RN05). Si un servicio no responde, registra FALLIDA y conserva EN_RIESGO (RN16).
     */
    @Transactional(noRollbackFor = ServicioNoDisponibleException.class)
    public Clase decidir(Clase clase, DecisionRiesgo decision, String responsable) {
        if (clase.getEstado() != EstadoClase.EN_RIESGO) {
            throw new ClaseNoEnRiesgoException("La clase " + clase.getId() + " no está EN_RIESGO (estado actual: "
                    + clase.getEstado() + ")");
        }
        if (decision == DecisionRiesgo.MANTENER) {
            cambioEstado.cambiarEstado(clase, clase.getEstadoPrevio(),
                    "El profesor decidió mantener la clase", responsable);
            return clase;
        }

        ActividadDto actividad;
        EspacioDto espacio;
        PlanReprogramacion plan;
        try {
            actividad = actividades.obtenerActividad(clase.getTipoActividadId());
            espacio = actividades.obtenerEspacio(clase.getEspacioId());
            plan = reprogramacion.buscarNuevoDia(clase, actividad, espacio);
        } catch (ServicioNoDisponibleException e) {
            // Evita que la transacción revierta el registro FALLIDA al propagar el 503 al cliente.
            evaluaciones.save(new EvaluacionClimatica(clase.getId(), LocalDateTime.now(reloj), clase.getInicio(),
                    ResultadoEvaluacion.FALLIDA,
                    "No se pudo completar la suspensión por riesgo: " + e.getMessage(),
                    null, null, null, null, null));
            throw e;
        }

        String clima = evaluaciones
                .findFirstByClaseIdAndResultadoNotOrderByFechaEvaluacionDesc(
                        clase.getId(), ResultadoEvaluacion.FALLIDA)
                .map(EvaluacionClimatica::getMotivo).orElse("riesgo climático");
        reprogramacion.suspenderYReprogramar(clase, actividad, plan,
                "El profesor suspendió la clase en riesgo (" + clima + ")", responsable);
        return clase;
    }
}
