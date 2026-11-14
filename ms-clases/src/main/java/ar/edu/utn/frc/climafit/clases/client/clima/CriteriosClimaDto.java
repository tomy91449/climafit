package ar.edu.utn.frc.climafit.clases.client.clima;

/** Criterios de la actividad para ms-clima. lluviaMaxMmHora es opcional (null = no se evalúa). */
public record CriteriosClimaDto(int probLluviaMax, int vientoMaxKmh, int tempMinC, Double lluviaMaxMmHora) {
}
