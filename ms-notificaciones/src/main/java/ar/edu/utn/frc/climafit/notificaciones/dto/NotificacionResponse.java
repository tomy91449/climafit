package ar.edu.utn.frc.climafit.notificaciones.dto;

import ar.edu.utn.frc.climafit.notificaciones.dominio.EstadoNotificacion;
import ar.edu.utn.frc.climafit.notificaciones.dominio.Notificacion;
import ar.edu.utn.frc.climafit.notificaciones.dominio.TipoEvento;

import java.time.LocalDateTime;

/** Una fila de GET /notificaciones?claseId=: el aviso a un destinatario, con los datos de su evento. */
public record NotificacionResponse(
        Long id,
        String eventoId,
        TipoEvento tipo,
        Long claseId,
        String usuarioId,
        String telegramChatId,
        String mensaje,
        EstadoNotificacion estado,
        int intentos,
        String ultimoError,
        String idMensajeProveedor,
        LocalDateTime fechaRecepcion,
        LocalDateTime fechaEnvio) {

    public static NotificacionResponse de(Notificacion n) {
        var e = n.getEvento();
        return new NotificacionResponse(n.getId(), e.getEventoId(), e.getTipo(), e.getClaseId(), n.getUsuarioId(),
                n.getTelegramChatId(), e.getMensaje(), n.getEstado(), n.getIntentos(), n.getUltimoError(),
                n.getIdMensajeProveedor(), e.getFechaRecepcion(), n.getFechaEnvio());
    }
}
