package ar.edu.utn.frc.climafit.clases.aviso;

/** Cliente de ms-notificaciones. */
public interface NotificacionesClient {

    /** Registra el evento (ms-notificaciones responde 202). Lanza una excepción si no se pudo entregar. */
    void enviar(EventoNotificacion evento);
}
