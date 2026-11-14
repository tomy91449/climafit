package ar.edu.utn.frc.climafit.clases.client.actividades;

/** Lo que devuelve GET /espacios/{id} (Fase 0, punto 8). */
public record EspacioDto(Long id, String nombre, String direccion, Double latitud, Double longitud,
                         boolean alAireLibre, Boolean activo) {

    public boolean estaActivo() {
        return !Boolean.FALSE.equals(activo);
    }
}
