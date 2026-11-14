package ar.edu.utn.frc.climafit.clases.clase;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReprogramacionRequest(@NotNull LocalDateTime nuevoInicio) {
}
