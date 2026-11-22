package ar.edu.utn.frc.climafit.notificaciones.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Proveedor de demo: registra el mensaje en el log en lugar de mandarlo. Permite mostrar el
 * proceso completo sin bot de Telegram ni usuarios que le hayan escrito. Es el proveedor por
 * defecto si no se configura ninguno.
 */
@Component
@ConditionalOnProperty(name = "climafit.telegram.proveedor", havingValue = "log", matchIfMissing = true)
public class ProveedorTelegramLog implements ProveedorTelegram {

    private static final Logger log = LoggerFactory.getLogger(ProveedorTelegramLog.class);

    @Override
    public String enviar(String chatId, String mensaje) {
        String id = "LOG-" + UUID.randomUUID();
        log.info("[Telegram simulado] al chat {}: {} (id {})", chatId, mensaje, id);
        return id;
    }
}
