package ar.edu.utn.frc.climafit.notificaciones.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Cuerpo de PUT /contactos/mio (F10).
 * <p>
 * Un bot de Telegram no puede escribirle a un número de teléfono: necesita el chat_id del usuario.
 * Para un chat privado es el id numérico del usuario en Telegram (por ejemplo 123456789), y existe
 * recién cuando el usuario le mandó /start al bot. Puede faltar solo si el usuario rechaza los avisos.
 */
public record ContactoRequest(
        @Pattern(regexp = "^-?\\d{1,20}$", message = "debe ser el chat_id numérico de Telegram, por ejemplo 123456789")
        String telegramChatId,
        @NotNull Boolean aceptaTelegram) {

    /** Aceptar avisos sin dar el chat no tiene sentido: se rechaza con 400 DATOS_INVALIDOS. */
    @JsonIgnore
    @AssertTrue(message = "para aceptar avisos por Telegram hay que registrar el chat_id")
    public boolean isChatPresenteSiAcepta() {
        return !Boolean.TRUE.equals(aceptaTelegram) || (telegramChatId != null && !telegramChatId.isBlank());
    }
}
