package ar.edu.utn.frc.climafit.notificaciones.servicio;

import ar.edu.utn.frc.climafit.notificaciones.dominio.EstadoNotificacion;
import ar.edu.utn.frc.climafit.notificaciones.repositorio.NotificacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Red de seguridad del envío asincrónico. El envío corre en memoria: si el contenedor se reinicia
 * después de registrar el evento pero antes de terminar de enviar, esas notificaciones quedarían
 * PENDIENTE para siempre. Esta tarea las busca cada tanto y las vuelve a procesar.
 * <p>
 * Solo toma las que tienen cierta antigüedad, para no pisarse con un envío que todavía está en
 * curso (un envío con todos sus reintentos tarda bastante menos que ese margen).
 */
@Component
public class RecuperacionPendientes {

    private static final Logger log = LoggerFactory.getLogger(RecuperacionPendientes.class);

    private final NotificacionRepository notificaciones;
    private final EnvioNotificaciones envio;
    private final Duration antiguedadMinima;

    public RecuperacionPendientes(NotificacionRepository notificaciones, EnvioNotificaciones envio,
                                  @Value("${climafit.notificaciones.pendientes-antiguedad:PT5M}") Duration antiguedadMinima) {
        this.notificaciones = notificaciones;
        this.envio = envio;
        this.antiguedadMinima = antiguedadMinima;
    }

    @Scheduled(fixedDelayString = "${climafit.notificaciones.recuperacion-cada:PT5M}",
            initialDelayString = "${climafit.notificaciones.recuperacion-cada:PT5M}")
    public void reenviarPendientes() {
        List<Long> pendientes = notificaciones.findIdsPorEstadoRecibidasAntesDe(
                EstadoNotificacion.PENDIENTE, LocalDateTime.now().minus(antiguedadMinima));
        if (!pendientes.isEmpty()) {
            log.warn("Reprocesando {} notificaciones que quedaron PENDIENTE", pendientes.size());
            pendientes.forEach(envio::reintentarPendiente);
        }
    }
}
