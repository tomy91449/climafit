package ar.edu.utn.frc.climafit.clases.aviso;

import java.util.List;

/** PROVISORIO (Persona 3, tarea 3.6): cliente de ms-notificaciones. */
public interface NotificacionesClient {

    /** POST /internal/notificaciones (responde 202). */
    void registrar(EventoNotificacion evento);

    record EventoNotificacion(String eventoId, TipoEvento tipo, Long claseId, String profesorId,
                              String mensaje, List<String> destinatarios) {
    }
}
