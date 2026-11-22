package ar.edu.utn.frc.climafit.notificaciones.dto;

import ar.edu.utn.frc.climafit.notificaciones.dominio.Contacto;

import java.time.LocalDateTime;

public record ContactoResponse(
        String usuarioId,
        String telegramChatId,
        boolean aceptaTelegram,
        LocalDateTime actualizado) {

    public static ContactoResponse de(Contacto c) {
        return new ContactoResponse(c.getUsuarioId(), c.getTelegramChatId(), c.isAceptaTelegram(), c.getActualizado());
    }
}
