package ar.edu.utn.frc.climafit.notificaciones.dto;

import ar.edu.utn.frc.climafit.notificaciones.dominio.Contacto;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Preferencias de contacto.
 * <ul>
 *   <li>{@code vinculado}: el bot ya conoce un chat del usuario y le puede enviar avisos.</li>
 *   <li>{@code linkVinculacion}: link al bot que hay que abrir y tocar "Iniciar". Aparece la primera
 *       vez que acepta avisos y cuando pide cambiar de cuenta (POST /contactos/mio/vinculacion).
 *       Mientras cambia de cuenta, {@code vinculado} sigue en true: el chat anterior se usa hasta que
 *       llegue el nuevo.</li>
 * </ul>
 * Sin bot real (proveedor "log") no hay link: el chat se carga a mano con telegramChatId.
 */
public record ContactoResponse(
        String usuarioId,
        String telegramChatId,
        boolean aceptaTelegram,
        boolean vinculado,
        String linkVinculacion,
        LocalDateTime linkVence,
        LocalDateTime actualizado) {

    public static ContactoResponse de(Contacto c, Optional<String> usuarioBot) {
        boolean conLink = c.isAceptaTelegram() && c.tieneCodigoVigente(LocalDateTime.now())
                && usuarioBot.isPresent();
        return new ContactoResponse(
                c.getUsuarioId(),
                c.getTelegramChatId(),
                c.isAceptaTelegram(),
                c.tieneChat(),
                conLink ? "https://t.me/" + usuarioBot.get() + "?start=" + c.getCodigoVinculacion() : null,
                conLink ? c.getCodigoVence() : null,
                c.getActualizado());
    }
}
