package ar.edu.utn.frc.climafit.notificaciones.dto;

import ar.edu.utn.frc.climafit.notificaciones.dominio.TipoEvento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Cuerpo de POST /internal/notificaciones: lo manda ms-clases con su token de servicio.
 * <p>
 * Ejemplo (suspensión de la clase 7, avisando a dos alumnos):
 * <pre>
 * {
 *   "eventoId": "clase-7-cambio-15",
 *   "tipo": "REPROGRAMACION",
 *   "claseId": 7,
 *   "profesorId": "8c1f...-sub-de-profe1",
 *   "mensaje": "Tu clase de Yoga del 15/11 a las 18:00 se pasó al 16/11 a las 18:00 por lluvia (90 %).",
 *   "destinatarios": ["sub-alumno1", "sub-alumno2"]
 * }
 * </pre>
 * ms-clases es quien sabe quiénes son los destinatarios (los inscriptos, o el profesor en una
 * ALERTA_RIESGO) y arma el texto; este servicio solo registra y envía.
 */
public record EventoNotificacionRequest(
        @NotBlank @Size(max = 100) String eventoId,
        @NotNull TipoEvento tipo,
        @NotNull @Positive Long claseId,
        @NotBlank @Size(max = 64) String profesorId,
        @NotBlank @Size(max = 500) String mensaje,
        @NotEmpty List<@NotBlank @Size(max = 64) String> destinatarios) {
}
