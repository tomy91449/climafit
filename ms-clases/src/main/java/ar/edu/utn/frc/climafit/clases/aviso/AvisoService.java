package ar.edu.utn.frc.climafit.clases.aviso;

import ar.edu.utn.frc.climafit.clases.estado.CambioEstado;
import ar.edu.utn.frc.climafit.clases.clase.ClaseRepository;
import ar.edu.utn.frc.climafit.commons.error.ServicioNoDisponibleException;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * PROVISORIO (Persona 3, tarea 3.6). Firma de la Fase 0, punto 9:
 * arma el evento, llama a ms-notificaciones y marca notificado (RN16: si falla, deja
 * notificado = false sin deshacer el cambio de estado).
 *
 * <p>Pendiente de acordar con la Persona 3: con la lista de destinatarios vacía (una clase
 * sin inscriptos) este provisorio marca notificado = true sin llamar a ms-notificaciones.
 */
@Service
public class AvisoService {

    private final NotificacionesClient notificaciones;
    private final ClaseRepository clases;

    public AvisoService(NotificacionesClient notificaciones, ClaseRepository clases) {
        this.notificaciones = notificaciones;
        this.clases = clases;
    }

    public void avisar(CambioEstado cambio, TipoEvento tipo, String mensaje, List<String> destinatarios) {
        if (destinatarios.isEmpty()) {
            cambio.setNotificado(true);
            return;
        }
        String eventoId = "CLASE-" + cambio.getClaseId() + "-CAMBIO-" + cambio.getId();
        String profesorId = clases.findById(cambio.getClaseId()).map(c -> c.getProfesorId()).orElse(null);
        try {
            notificaciones.registrar(new NotificacionesClient.EventoNotificacion(
                    eventoId, tipo, cambio.getClaseId(), profesorId, mensaje, destinatarios));
            cambio.setNotificado(true);
        } catch (ServicioNoDisponibleException e) {
            cambio.setNotificado(false);
        }
    }

    /** Reenvía los cambios con notificado = false con el mismo eventoId (RN15 evita duplicados). */
    public void reenviarPendientes() {
        // PROVISORIO: la implementación real es de la Persona 3. La tarea programada de la
        // Persona 4 solo la invoca antes de evaluar.
    }
}
