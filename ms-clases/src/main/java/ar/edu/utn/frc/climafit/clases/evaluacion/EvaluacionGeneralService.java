package ar.edu.utn.frc.climafit.clases.evaluacion;

import ar.edu.utn.frc.climafit.clases.clase.Clase;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Evaluación general de las próximas 24 h. La usan la tarea programada y
 * POST /clases/evaluaciones (misma lógica, como pide la propuesta).
 */
@Service
public class EvaluacionGeneralService {

    private static final Logger log = LoggerFactory.getLogger(EvaluacionGeneralService.class);

    private final ClasesAEvaluarRepository clases;
    private final EvaluacionService evaluacion;
    private final Clock reloj;

    public EvaluacionGeneralService(ClasesAEvaluarRepository clases, EvaluacionService evaluacion, Clock reloj) {
        this.clases = clases;
        this.evaluacion = evaluacion;
        this.reloj = reloj;
    }

    /**
     * P1 (RN09): selecciona las clases al aire libre que empiezan dentro de las próximas 24 horas
     * y las evalúa una por una, cada una en su propia transacción: el error de una no frena a las otras.
     */
    public ResumenEvaluacionGeneral ejecutar(String responsable) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        List<Clase> aEvaluar = clases.buscarAEvaluar(EvaluacionService.estadosEvaluables(), ahora,
                ahora.plusHours(EvaluacionService.HORAS_DE_ANTICIPACION));

        int aptas = 0, enRiesgo = 0, noAptas = 0, fallidas = 0, conError = 0;
        for (Clase clase : aEvaluar) {
            try {
                switch (evaluacion.evaluar(clase, responsable).resultado()) {
                    case APTA -> aptas++;
                    case EN_RIESGO -> enRiesgo++;
                    case NO_APTA -> noAptas++;
                    case FALLIDA -> fallidas++;
                }
            } catch (RuntimeException e) {
                conError++;
                log.warn("No se pudo evaluar la clase {}: {}", clase.getId(), e.getMessage());
            }
        }
        log.info("Evaluación general: {} clases ({} aptas, {} en riesgo, {} no aptas, {} fallidas, {} con error)",
                aEvaluar.size(), aptas, enRiesgo, noAptas, fallidas, conError);
        return new ResumenEvaluacionGeneral(aEvaluar.size(), aptas, enRiesgo, noAptas, fallidas, conError);
    }
}
