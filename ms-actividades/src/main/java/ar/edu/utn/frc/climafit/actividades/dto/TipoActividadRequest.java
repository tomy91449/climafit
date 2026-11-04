package ar.edu.utn.frc.climafit.actividades.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de POST /actividades. Las anotaciones de Bean Validation se verifican antes de entrar
 * al servicio; si alguna falla, climafit-commons responde 400 DATOS_INVALIDOS con el detalle.
 */
public record TipoActividadRequest(
        @NotBlank @Size(max = 80) String nombre,
        @Size(max = 255) String descripcion,
        @NotNull @Min(0) @Max(100) Integer probLluviaMax,
        @NotNull @Min(0) @Max(200) Integer vientoMaxKmh,
        @NotNull @Min(-30) @Max(50) Integer tempMinC) {
}
