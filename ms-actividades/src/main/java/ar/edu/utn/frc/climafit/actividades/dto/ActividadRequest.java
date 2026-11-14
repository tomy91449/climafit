package ar.edu.utn.frc.climafit.actividades.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Cuerpo de POST y PUT /actividades. {@code probLluviaMax}: probabilidad (%) a partir de la cual se
 * considera que va a llover; {@code lluviaMaxMmHora}: milímetros por hora que la actividad tolera.
 */
public record ActividadRequest(
        @NotBlank @Size(max = 100) String nombre,
        @Size(max = 500) String descripcion,
        @NotNull @Min(0) @Max(100) Integer probLluviaMax,
        @NotNull @DecimalMin("0.0") @DecimalMax("999.9") @Digits(integer = 3, fraction = 1) BigDecimal lluviaMaxMmHora,
        @NotNull @Min(0) Integer vientoMaxKmh,
        @NotNull Integer tempMinC) {
}
