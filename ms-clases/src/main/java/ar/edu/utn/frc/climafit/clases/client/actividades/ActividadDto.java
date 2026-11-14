package ar.edu.utn.frc.climafit.clases.client.actividades;

/** Lo que devuelve GET /actividades/{id} (Fase 0, punto 8). Se ignoran los campos que ms-clases no usa. */
public record ActividadDto(Long id, String nombre, String descripcion, Double probLluviaMax,
                           Double vientoMaxKmh, Double tempMinC, Double lluviaMaxMmHora,
                           Boolean activo) {

    public boolean estaActiva() {
        return !Boolean.FALSE.equals(activo);
    }
}
