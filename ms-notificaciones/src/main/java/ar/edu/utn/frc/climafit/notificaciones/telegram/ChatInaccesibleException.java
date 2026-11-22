package ar.edu.utn.frc.climafit.notificaciones.telegram;

/**
 * Telegram no deja escribirle a ese chat: el usuario bloqueó al bot (403) o el chat ya no existe
 * (400 "chat not found"). Reintentar no sirve, así que el envío falla de inmediato y el chat se
 * desvincula del contacto: la próxima vez que el usuario acepte avisos recibe un link nuevo.
 */
public class ChatInaccesibleException extends EnvioTelegramException {

    public ChatInaccesibleException(String mensaje) {
        super(mensaje);
    }
}
