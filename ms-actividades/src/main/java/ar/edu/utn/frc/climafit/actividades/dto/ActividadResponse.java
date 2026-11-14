package ar.edu.utn.frc.climafit.actividades.dto;

import ar.edu.utn.frc.climafit.actividades.entity.TipoActividad;

import java.math.BigDecimal;

/** Tipo de actividad con sus criterios climáticos. Es lo que lee ms-clases (Fase 0, punto 9). */
public record ActividadResponse(
        Long id,
        String nombre,
        String descripcion,
        Integer probLluviaMax,
        BigDecimal lluviaMaxMmHora,
        Integer vientoMaxKmh,
        Integer tempMinC,
        Boolean activo) {

    public static ActividadResponse de(TipoActividad actividad) {
        return new ActividadResponse(actividad.getId(), actividad.getNombre(), actividad.getDescripcion(),
                actividad.getProbLluviaMax(), actividad.getLluviaMaxMm(), actividad.getVientoMaxKmh(), actividad.getTempMinC(),
                actividad.getActivo());
    }
}
