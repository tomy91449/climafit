package ar.edu.utn.frc.climafit.clases.evaluacion;

/**
 * Qué pasó en una evaluación general. {@code conError} cuenta las clases que dieron un error
 * inesperado (por ejemplo, un conflicto de versión con el profesor): no frenan a las demás.
 */
public record ResumenEvaluacionGeneral(int evaluadas, int aptas, int enRiesgo, int noAptas, int fallidas,
                                       int conError) {
}
