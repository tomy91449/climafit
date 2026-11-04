package ar.edu.utn.frc.climafit.actividades.dto;

import ar.edu.utn.frc.climafit.actividades.dominio.TipoActividad;

public record TipoActividadResponse(
        Long id,
        String nombre,
        String descripcion,
        int probLluviaMax,
        int vientoMaxKmh,
        int tempMinC,
        boolean activo) {

    public static TipoActividadResponse de(TipoActividad t) {
        return new TipoActividadResponse(t.getId(), t.getNombre(), t.getDescripcion(),
                t.getProbLluviaMax(), t.getVientoMaxKmh(), t.getTempMinC(), t.isActivo());
    }
}
