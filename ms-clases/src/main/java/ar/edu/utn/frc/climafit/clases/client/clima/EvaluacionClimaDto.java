package ar.edu.utn.frc.climafit.clases.client.clima;

import ar.edu.utn.frc.climafit.clases.evaluacion.ResultadoEvaluacion;
import java.time.LocalDateTime;

/** Una evaluación devuelta por ms-clima (Fase 0, punto 9) más los mm de lluvia por hora. */
public record EvaluacionClimaDto(LocalDateTime inicio, ResultadoEvaluacion resultado, String motivo,
                                 int probLluvia, int vientoKmh, int temperaturaC, boolean tormenta,
                                 Double precipitacionMm) {
}
