package ar.edu.utn.frc.climafit.clases.aviso;

import ar.edu.utn.frc.climafit.commons.error.ServicioNoDisponibleException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Implementación falsa (perfil stub): guarda los eventos en una lista para poder verificarlos en los tests. */
@Component
@Profile("stub")
public class NotificacionesClientStub implements NotificacionesClient {

    private final List<EventoNotificacion> eventos = new CopyOnWriteArrayList<>();
    private volatile boolean fallar;

    @Override
    public void enviar(EventoNotificacion evento) {
        if (fallar) {
            throw new ServicioNoDisponibleException("ms-notificaciones simulado: no disponible");
        }
        eventos.add(evento);
    }

    /** Eventos recibidos hasta ahora. */
    public List<EventoNotificacion> eventos() {
        return List.copyOf(eventos);
    }

    /** Si es true, los próximos envíos fallan (para probar RN16). */
    public void fallar(boolean fallar) {
        this.fallar = fallar;
    }

    public void limpiar() {
        eventos.clear();
        fallar = false;
    }
}
