package ar.edu.utn.frc.climafit.notificaciones.servicio;

import ar.edu.utn.frc.climafit.notificaciones.dominio.Contacto;
import ar.edu.utn.frc.climafit.notificaciones.dominio.Notificacion;
import ar.edu.utn.frc.climafit.notificaciones.repositorio.ContactoRepository;
import ar.edu.utn.frc.climafit.notificaciones.repositorio.NotificacionRepository;
import ar.edu.utn.frc.climafit.notificaciones.whatsapp.ProveedorWhatsapp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.util.Optional;

/**
 * Envío de las notificaciones por WhatsApp, en segundo plano (F14, RN14, RN16).
 * <p>
 * Está en una clase aparte de {@link NotificacionService} porque {@code @Async} funciona a través
 * del proxy de Spring: si un método del mismo bean se llamara a sí mismo, se ejecutaría en el
 * mismo hilo.
 * <p>
 * No usa una transacción larga a propósito: entre intento e intento hay esperas de segundos, y no
 * conviene tener una conexión a la base tomada todo ese tiempo. Cada cambio se guarda por separado,
 * así el estado se ve actualizado mientras se reintenta.
 */
@Component
public class EnvioNotificaciones {

    private static final Logger log = LoggerFactory.getLogger(EnvioNotificaciones.class);

    private final NotificacionRepository notificaciones;
    private final ContactoRepository contactos;
    private final ProveedorWhatsapp proveedor;
    private final int maxIntentos;
    private final Duration esperaInicial;

    public EnvioNotificaciones(NotificacionRepository notificaciones, ContactoRepository contactos,
                               ProveedorWhatsapp proveedor,
                               @Value("${climafit.whatsapp.max-intentos:3}") int maxIntentos,
                               @Value("${climafit.whatsapp.espera-inicial:PT1S}") Duration esperaInicial) {
        this.notificaciones = notificaciones;
        this.contactos = contactos;
        this.proveedor = proveedor;
        this.maxIntentos = maxIntentos;
        this.esperaInicial = esperaInicial;
    }

    /** Se ejecuta cuando la transacción de {@link NotificacionService#registrarEvento} se confirmó. */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alConfirmarse(NotificacionService.NotificacionesCreadas evento) {
        evento.ids().forEach(this::procesar);
    }

    /** Para la tarea de recuperación: reenvía una PENDIENTE que quedó colgada. */
    @Async
    public void reintentarPendiente(Long notificacionId) {
        procesar(notificacionId);
    }

    /**
     * Procesa UNA notificación: decide si se omite (RN14) o se envía con reintentos (RN16).
     * Es público para poder probarlo sin hilos en los tests.
     */
    public void procesar(Long notificacionId) {
        Optional<Notificacion> encontrada = notificaciones.findConEvento(notificacionId);
        if (encontrada.isEmpty() || !encontrada.get().estaPendiente()) {
            return; // ya se procesó (por ejemplo, la tarea de recuperación llegó tarde)
        }
        Notificacion notificacion = encontrada.get();

        // RN14: el consentimiento se mira al momento de enviar, con el contacto vigente.
        Optional<Contacto> contacto = contactos.findById(notificacion.getUsuarioId());
        if (contacto.isEmpty() || !contacto.get().puedeRecibirWhatsapp()) {
            String motivo = contacto.isEmpty() ? "El destinatario no registró sus preferencias de contacto"
                    : !contacto.get().isAceptaWhatsapp() ? "El destinatario no aceptó avisos por WhatsApp"
                    : "El destinatario no tiene teléfono registrado";
            notificacion.omitir(motivo);
            notificaciones.save(notificacion);
            log.info("Notificación {} OMITIDA: {}", notificacionId, motivo);
            return;
        }

        enviarConReintentos(notificacion, contacto.get().getTelefono());
    }

    /**
     * RN16: hasta {@code maxIntentos} intentos con espera creciente (1 s, 2 s, 4 s...). Si todos
     * fallan la notificación queda FALLIDA. La clase NO se toca: el cambio ya ocurrió y es válido
     * aunque el aviso no haya llegado.
     */
    private void enviarConReintentos(Notificacion notificacion, String telefono) {
        String mensaje = notificacion.getEvento().getMensaje();
        Duration espera = esperaInicial;

        while (true) {
            notificacion.registrarIntento(telefono);
            try {
                String idProveedor = proveedor.enviar(telefono, mensaje);
                notificacion.marcarEnviada(idProveedor);
                notificaciones.save(notificacion);
                log.info("Notificación {} ENVIADA en el intento {}", notificacion.getId(), notificacion.getIntentos());
                return;
            } catch (RuntimeException ex) {
                log.warn("Intento {}/{} de la notificación {} falló: {}", notificacion.getIntentos(), maxIntentos,
                        notificacion.getId(), ex.getMessage());
                if (notificacion.getIntentos() >= maxIntentos) {
                    notificacion.marcarFallida(ex.getMessage());
                    notificaciones.save(notificacion);
                    log.error("Notificación {} FALLIDA después de {} intentos", notificacion.getId(), maxIntentos);
                    return;
                }
                notificacion.registrarError(ex.getMessage());
                notificaciones.save(notificacion);
            }
            if (!esperar(espera)) {
                return; // el servicio se está apagando: queda PENDIENTE y la recupera la tarea programada
            }
            espera = espera.multipliedBy(2);
        }
    }

    private boolean esperar(Duration espera) {
        try {
            Thread.sleep(espera);
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
