package ar.edu.utn.frc.climafit.actividades.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Cuerpo de POST /ejercicios/compatibilidad (Fase 0, punto 9). */
public record CompatibilidadRequest(
        @NotNull Long actividadId,
        @NotEmpty List<@NotNull Long> ejercicioIds) {
}
