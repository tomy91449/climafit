package ar.edu.utn.frc.climafit.notificaciones.telegram;

/** El proveedor no pudo enviar el mensaje. Se reintenta según RN16. */
public class EnvioTelegramException extends RuntimeException {

    public EnvioTelegramException(String mensaje) {
        super(mensaje);
    }

    public EnvioTelegramException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
