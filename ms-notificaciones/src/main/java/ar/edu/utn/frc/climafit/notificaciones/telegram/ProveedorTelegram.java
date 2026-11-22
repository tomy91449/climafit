package ar.edu.utn.frc.climafit.notificaciones.telegram;

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
     * @throws EnvioTelegramException si el proveedor rechazó el mensaje o no respondió
     */
    String enviar(String chatId, String mensaje);
}
