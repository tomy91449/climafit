package ar.edu.utn.frc.climafit.clases.evaluacion;

import ar.edu.utn.frc.climafit.clases.estado.EstadoClase;
import java.time.LocalDateTime;

/**
 * Cuerpo de POST /clases/{id}/evaluaciones (Fase 0, punto 8). Cuando la evaluación es FALLIDA
 * no hay valores de pronóstico (quedan null) y la clase conserva su estado.
 */
public record EvaluacionRespuesta(ResultadoEvaluacion resultado, String motivo, Integer probLluvia,
                                  Integer vientoKmh, Integer temperaturaC, Boolean tormenta,
                                  Double precipitacionMm, EstadoClase estadoClase,
                                  LocalDateTime nuevoInicio) {
}
