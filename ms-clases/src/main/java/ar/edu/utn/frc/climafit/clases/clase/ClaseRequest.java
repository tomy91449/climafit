package ar.edu.utn.frc.climafit.clases.clase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cuerpo de POST /clases y PUT /clases/{id} (Fase 0, punto 8).
 * En el PUT solo se aplican cupo, duracionMinutos y rutina; tipoActividadId, espacioId e inicio se ignoran
 * (para mover la clase de fecha está PATCH /clases/{id}/reprogramacion).
 */
public record ClaseRequest(
        @NotNull(groups = Crear.class) Long tipoActividadId,
        @NotNull(groups = Crear.class) Long espacioId,
        @NotNull(groups = Crear.class) LocalDateTime inicio,
        @NotNull(groups = Crear.class) @Min(1) @Max(Clase.DURACION_MAX_MIN) Integer duracionMinutos,
        @NotNull(groups = Crear.class) @Min(1) Integer cupo,
        @Valid List<RutinaItemDto> rutina) {
}
