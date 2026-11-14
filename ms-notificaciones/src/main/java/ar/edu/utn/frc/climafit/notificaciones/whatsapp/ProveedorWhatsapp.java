package ar.edu.utn.frc.climafit.notificaciones.whatsapp;

/**
 * Canal de envío. Hay dos implementaciones y se elige con la propiedad
 * {@code climafit.whatsapp.proveedor} (variable de entorno WHATSAPP_PROVEEDOR):
 * <ul>
 *   <li>{@code log}: no manda nada, solo escribe el mensaje en el log. Es la de la demo.</li>
 *   <li>{@code twilio}: manda el mensaje real por la API de Twilio (sandbox de WhatsApp).</li>
 * </ul>
 * El resto del servicio solo conoce esta interfaz, así que cambiar de proveedor no toca la lógica.
 */
public interface ProveedorWhatsapp {

    /**
     * Envía un mensaje.
     *
     * @param telefono destino en formato E.164 (+5493511234567)
     * @return identificador del mensaje en el proveedor
     * @throws EnvioWhatsappException si el proveedor rechazó el mensaje o no respondió
     */
    String enviar(String telefono, String mensaje);
}
