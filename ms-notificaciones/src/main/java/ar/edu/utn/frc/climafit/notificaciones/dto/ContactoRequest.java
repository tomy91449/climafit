package ar.edu.utn.frc.climafit.notificaciones.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Cuerpo de PUT /contactos/mio (F10).
 * <p>
 * Lo normal es mandar solo {@code {"aceptaTelegram": true}}: la respuesta trae un link al bot y,
 * cuando el usuario toca "Iniciar", el chat se registra solo. Un bot de Telegram no puede
 * escribirle a un número de teléfono: necesita el chat_id, que existe recién cuando el usuario le
 * escribió al bot.
 * <p>
 * {@code telegramChatId} es opcional y sirve para cargar el chat a mano (pruebas y demo).
 */
public record ContactoRequest(
        @Pattern(regexp = "^-?\\d{1,20}$", message = "debe ser el chat_id numérico de Telegram, por ejemplo 123456789")
        String telegramChatId,
        @NotNull Boolean aceptaTelegram) {
}
