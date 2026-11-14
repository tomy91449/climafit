package ar.edu.utn.frc.climafit.notificaciones.whatsapp;

/** El proveedor no pudo enviar el mensaje. Se reintenta según RN16. */
public class EnvioWhatsappException extends RuntimeException {

    public EnvioWhatsappException(String mensaje) {
        super(mensaje);
    }

    public EnvioWhatsappException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
