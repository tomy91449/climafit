package ar.edu.utn.frc.climafit.notificaciones.servicio;

import ar.edu.utn.frc.climafit.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.climafit.commons.seguridad.UsuarioActual;
import ar.edu.utn.frc.climafit.notificaciones.dominio.EventoNotificacion;
import ar.edu.utn.frc.climafit.notificaciones.dominio.Notificacion;
import ar.edu.utn.frc.climafit.notificaciones.dto.EventoNotificacionRequest;
import ar.edu.utn.frc.climafit.notificaciones.dto.EventoNotificacionResponse;
import ar.edu.utn.frc.climafit.notificaciones.dto.NotificacionResponse;
import ar.edu.utn.frc.climafit.notificaciones.repositorio.EventoNotificacionRepository;
import ar.edu.utn.frc.climafit.notificaciones.repositorio.NotificacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Registro de eventos y consulta de notificaciones (F14).
 * <p>
 * El registro es rápido a propósito: guarda el evento y una notificación PENDIENTE por destinatario
 * y termina. ms-clases recibe el 202 al instante y no queda esperando a Telegram. El envío lo hace
 * {@link EnvioNotificaciones} en otro hilo, recién cuando la transacción se confirmó.
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    private final EventoNotificacionRepository eventos;
    private final NotificacionRepository notificaciones;
    private final ApplicationEventPublisher publicador;
    private final UsuarioActual usuarioActual;

    public NotificacionService(EventoNotificacionRepository eventos, NotificacionRepository notificaciones,
                               ApplicationEventPublisher publicador, UsuarioActual usuarioActual) {
        this.eventos = eventos;
        this.notificaciones = notificaciones;
        this.publicador = publicador;
        this.usuarioActual = usuarioActual;
    }

    /**
     * RN15: si el evento ya existe (ms-clases reintentó porque no le llegó la respuesta), no se crea
     * de nuevo y solo se agregan los destinatarios que todavía no tenían notificación. Repetir el
     * pedido completo no es un error: se ignora y se responde igual 202.
     */
    @Transactional
    public EventoNotificacionResponse registrarEvento(EventoNotificacionRequest request) {
        EventoNotificacion evento = eventos.findById(request.eventoId())
                .orElseGet(() -> eventos.save(new EventoNotificacion(request.eventoId(), request.tipo(),
                        request.claseId(), request.profesorId(), request.mensaje())));

        // Sin repetidos dentro del mismo pedido, manteniendo el orden.
        Set<String> destinatarios = new LinkedHashSet<>(request.destinatarios());
        Set<String> yaNotificados = notificaciones.findUsuariosNotificados(evento.getEventoId());

        List<Long> creadas = new ArrayList<>();
        for (String usuarioId : destinatarios) {
            if (!yaNotificados.contains(usuarioId)) {
                creadas.add(notificaciones.save(new Notificacion(evento, usuarioId)).getId());
            }
        }
        int ignoradas = request.destinatarios().size() - creadas.size();

        log.info("Evento {} ({}) de la clase {}: {} notificaciones nuevas, {} duplicadas ignoradas",
                evento.getEventoId(), evento.getTipo(), evento.getClaseId(), creadas.size(), ignoradas);

        // Se envían DESPUÉS del commit (ver EnvioNotificaciones#alConfirmarse): si se mandara antes y
        // la transacción fallara, el alumno recibiría un aviso de algo que no quedó registrado.
        if (!creadas.isEmpty()) {
            publicador.publishEvent(new NotificacionesCreadas(creadas));
        }
        return new EventoNotificacionResponse(evento.getEventoId(), creadas.size(), ignoradas);
    }

    /**
     * Notificaciones de una clase. Un ADMIN ve las de cualquiera; un PROFESOR solo las de sus clases
     * (RN05). El dueño se sabe por el profesorId que mandó ms-clases con cada evento, así que no hace
     * falta consultar a ms-clases.
     */
    @Transactional(readOnly = true)
    public List<NotificacionResponse> listarPorClase(Long claseId) {
        List<Notificacion> lista = notificaciones.findByClase(claseId);
        if (!usuarioActual.esAdmin()) {
            String yo = usuarioActual.id();
            boolean esMia = lista.stream().allMatch(n -> n.getEvento().getProfesorId().equals(yo));
            if (!esMia) {
                throw new AccesoDenegadoException("Solo el profesor de la clase puede ver sus notificaciones");
            }
        }
        return lista.stream().map(NotificacionResponse::de).toList();
    }

    /** Evento interno de Spring (no confundir con EventoNotificacion): ids de las notificaciones a enviar. */
    public record NotificacionesCreadas(List<Long> ids) {
    }
}
