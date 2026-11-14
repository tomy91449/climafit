package ar.edu.utn.frc.climafit.clases.clase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelacionRequest(@NotBlank @Size(max = 500) String motivo) {
}
