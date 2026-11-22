package ar.edu.utn.frc.climafit.notificaciones.telegram;

import java.util.Optional;

/**
 * Canal de envío de los avisos. Hay dos implementaciones y se elige con la propiedad
 * {@code climafit.telegram.proveedor} (variable de entorno TELEGRAM_PROVEEDOR):
 * <ul>
 *   <li>{@code log}: no manda nada, solo escribe el mensaje en el log. Es la de la demo.</li>
 *   <li>{@code bot}: manda el mensaje real con la Bot API de Telegram.</li>
 * </ul>
 * El resto del servicio solo conoce esta interfaz, así que cambiar de proveedor no toca la lógica.
 */
public interface ProveedorTelegram {

    /**
     * Envía un mensaje.
     *
     * @param chatId chat de Telegram del destinatario (número; existe recién cuando el usuario le
     *               mandó /start al bot)
     * @return identificador del mensaje en el proveedor
     * @throws ChatInaccesibleException si el usuario bloqueó al bot o el chat ya no existe
     * @throws EnvioTelegramException   si el proveedor rechazó el mensaje o no respondió
     */
    String enviar(String chatId, String mensaje);

    /**
     * Usuario del bot (sin @), para armar el link de vinculación {@code t.me/<usuario>?start=<código>}.
     * Vacío si no hay un bot real (proveedor "log") o si Telegram no respondió: en ese caso no se
     * ofrece link y el chat se puede cargar a mano.
     */
    default Optional<String> usuarioBot() {
        return Optional.empty();
    }
}
