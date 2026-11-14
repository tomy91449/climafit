package ar.edu.utn.frc.climafit.clima.modelo;

/**
 * Criterios de una actividad (los que manda ms-clases en /internal/evaluaciones).
 *
 * @param lluviaMaxMmHora máximo de mm de agua por hora que soporta la actividad. Es opcional:
 *                        si es null no se evalúa (así sigue funcionando el contrato original
 *                        de la propuesta y la colección de Postman).
 */
public record CriteriosClimaticos(int probLluviaMax, int vientoMaxKmh, int tempMinC,
                                  Double lluviaMaxMmHora) {
}
