package ar.edu.utn.frc.climafit.actividades.dto;

import ar.edu.utn.frc.climafit.actividades.entity.Nivel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Cuerpo de POST y PUT /ejercicios. */
public record EjercicioRequest(
        @NotBlank @Size(max = 100) String nombre,
        @Size(max = 1000) String descripcion,
        @NotNull Nivel nivel,
        @NotEmpty List<@NotNull Long> actividadesCompatibles) {
}
