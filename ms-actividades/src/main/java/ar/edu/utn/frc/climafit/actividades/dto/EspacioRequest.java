package ar.edu.utn.frc.climafit.actividades.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Cuerpo de POST y PUT /espacios. */
public record EspacioRequest(
        @NotBlank @Size(max = 100) String nombre,
        @Size(max = 255) String direccion,
        @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitud,
        @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitud,
        @NotNull Boolean alAireLibre) {
}
