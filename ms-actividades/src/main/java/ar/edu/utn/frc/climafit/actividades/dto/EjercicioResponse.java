package ar.edu.utn.frc.climafit.actividades.dto;

import ar.edu.utn.frc.climafit.actividades.entity.Ejercicio;
import ar.edu.utn.frc.climafit.actividades.entity.Nivel;
import ar.edu.utn.frc.climafit.actividades.entity.TipoActividad;

import java.util.List;

/** Ejercicio con las actividades con las que es compatible. {@code propietarioId} es null en los ejercicios base. */
public record EjercicioResponse(
        Long id,
        String nombre,
        String descripcion,
        Nivel nivel,
        String propietarioId,
        List<Long> actividadesCompatibles,
        Boolean activo) {

    public static EjercicioResponse de(Ejercicio ejercicio) {
        List<Long> actividades = ejercicio.getActividades().stream()
                .map(TipoActividad::getId)
                .sorted()
                .toList();
        return new EjercicioResponse(ejercicio.getId(), ejercicio.getNombre(), ejercicio.getDescripcion(),
                ejercicio.getNivel(), ejercicio.getPropietarioId(), actividades, ejercicio.getActivo());
    }
}
