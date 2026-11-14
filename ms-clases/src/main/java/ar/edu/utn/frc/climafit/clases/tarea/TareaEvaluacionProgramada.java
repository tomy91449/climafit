package ar.edu.utn.frc.climafit.clases.tarea;

import ar.edu.utn.frc.climafit.clases.aviso.AvisoService;
import ar.edu.utn.frc.climafit.clases.evaluacion.EvaluacionGeneralService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tarea 4.9 - cada 3 horas: primero reenvía los avisos pendientes (RN16, a cargo de la
 * Persona 3) y después evalúa todas las clases que cumplen RN09, con la misma lógica que
 * POST /clases/evaluaciones. Se apaga con climafit.evaluacion.programada=false.
 */
@Component
@ConditionalOnProperty(name = "climafit.evaluacion.programada", havingValue = "true", matchIfMissing = true)
public class TareaEvaluacionProgramada {

    private static final Logger log = LoggerFactory.getLogger(TareaEvaluacionProgramada.class);

    static final String RESPONSABLE_SISTEMA = "SISTEMA";

    private final AvisoService avisos;
    private final EvaluacionGeneralService evaluacionGeneral;

    public TareaEvaluacionProgramada(AvisoService avisos, EvaluacionGeneralService evaluacionGeneral) {
        this.avisos = avisos;
        this.evaluacionGeneral = evaluacionGeneral;
    }

    @Scheduled(fixedRateString = "${climafit.evaluacion.intervalo:PT3H}",
            initialDelayString = "${climafit.evaluacion.retraso-inicial:PT1M}")
    public void ejecutar() {
        try {
            avisos.reenviarPendientes();
        } catch (RuntimeException e) {
            // Un reenvío fallido no puede impedir que se evalúen las clases.
            log.warn("No se pudieron reenviar los avisos pendientes: {}", e.getMessage());
        }
        evaluacionGeneral.ejecutar(RESPONSABLE_SISTEMA);
    }
}
