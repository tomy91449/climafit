package ar.edu.utn.frc.climafit.clases.clase;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Un ejercicio de la rutina: ejercicioId, orden, indicaciones (Fase 0, punto 8). */
public record RutinaItemDto(@NotNull Long ejercicioId, @NotNull @Min(1) Integer orden,
                            @Size(max = 500) String indicaciones) {
}
