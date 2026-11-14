package ar.edu.utn.frc.climafit.clases.aviso;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * PROVISORIO (Persona 3, tarea 3.6): sin ms-notificaciones, solo registra el evento en el log.
 * Se reemplaza por el cliente real (POST /internal/notificaciones con token de servicio).
 */
@Component
public class NotificacionesClientLog implements NotificacionesClient {

    private static final Logger log = LoggerFactory.getLogger(NotificacionesClientLog.class);

    @Override
    public void registrar(EventoNotificacion evento) {
        log.info("[PROVISORIO] Evento {} ({}) para {}: {}", evento.eventoId(), evento.tipo(),
                evento.destinatarios(), evento.mensaje());
    }
}
