package ar.edu.utn.frc.climafit.clima.modelo;

import java.time.LocalDateTime;

/** Resultado de evaluar una ventana horaria: el veredicto y los valores que lo decidieron. */
public record EvaluacionVentana(LocalDateTime inicio, ResultadoClima resultado, String motivo,
                                int probLluvia, int vientoKmh, int temperaturaC, boolean tormenta,
                                double precipitacionMm) {
}
