package ar.edu.utn.frc.climafit.clases.client.actividades;

/**
 * PROVISORIO (Persona 3, tarea 3.3). Criterios climáticos de la actividad.
 * lluviaMaxMmHora es el campo NUEVO del cambio de dominio (null = sin criterio de mm).
 * El nombre es provisorio: se acuerda con los 4 en docs/contratos.md.
 */
public record ActividadDto(Long id, String nombre, int probLluviaMax, int vientoMaxKmh, int tempMinC,
                           Double lluviaMaxMmHora, boolean activo) {
}
