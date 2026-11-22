package ar.edu.utn.frc.climafit.notificaciones.dto;

import ar.edu.utn.frc.climafit.notificaciones.dominio.Contacto;

import java.time.LocalDateTime;

/**
 * Preferencias de contacto. Si el usuario aceptó avisos pero todavía no vinculó su Telegram,
 * {@code linkVinculacion} trae el link al bot que tiene que abrir y tocar "Iniciar".
 * Una vez vinculado, el link vuelve null y {@code vinculado} pasa a true.
 */
public record ContactoResponse(
        String usuarioId,
        String telegramChatId,
        boolean aceptaTelegram,
        boolean vinculado,
        String linkVinculacion,
        LocalDateTime linkVence,
        LocalDateTime actualizado) {

    public static ContactoResponse de(Contacto c, String usuarioBot) {
        boolean conLink = c.isAceptaTelegram() && !c.tieneChat()
                && c.tieneCodigoVigente(LocalDateTime.now());
        return new ContactoResponse(
                c.getUsuarioId(),
                c.getTelegramChatId(),
                c.isAceptaTelegram(),
                c.tieneChat(),
                conLink ? "https://t.me/" + usuarioBot + "?start=" + c.getCodigoVinculacion() : null,
                conLink ? c.getCodigoVence() : null,
                c.getActualizado());
    }
}
