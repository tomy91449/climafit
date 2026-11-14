package ar.edu.utn.frc.climafit.notificaciones.dto;

/**
 * Respuesta del 202 Accepted: el evento quedó registrado; los mensajes se mandan en segundo plano.
 *
 * @param notificacionesCreadas destinatarios nuevos para los que se programó un envío
 * @param duplicadasIgnoradas   destinatarios que ya tenían notificación de este evento (RN15)
 */
public record EventoNotificacionResponse(
        String eventoId,
        int notificacionesCreadas,
        int duplicadasIgnoradas) {
}
