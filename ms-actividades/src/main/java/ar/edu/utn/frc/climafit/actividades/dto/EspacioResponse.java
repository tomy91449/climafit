package ar.edu.utn.frc.climafit.actividades.dto;

import ar.edu.utn.frc.climafit.actividades.entity.Espacio;

import java.math.BigDecimal;

/** Espacio con sus coordenadas. Es lo que lee ms-clases (Fase 0, punto 9). */
public record EspacioResponse(
        Long id,
        String nombre,
        String direccion,
        BigDecimal latitud,
        BigDecimal longitud,
        Boolean alAireLibre,
        Boolean activo) {

    public static EspacioResponse de(Espacio espacio) {
        return new EspacioResponse(espacio.getId(), espacio.getNombre(), espacio.getDireccion(),
                espacio.getLatitud(), espacio.getLongitud(), espacio.getAlAireLibre(), espacio.getActivo());
    }
}
