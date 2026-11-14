package ar.edu.utn.frc.climafit.notificaciones.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Proveedor de demo: registra el mensaje en el log en lugar de mandarlo. Permite mostrar el
 * proceso completo sin cuenta de Twilio ni teléfonos unidos al sandbox (riesgo previsto en la
 * propuesta). Es el proveedor por defecto si no se configura ninguno.
 */
@Component
@ConditionalOnProperty(name = "climafit.whatsapp.proveedor", havingValue = "log", matchIfMissing = true)
public class ProveedorWhatsappLog implements ProveedorWhatsapp {

    private static final Logger log = LoggerFactory.getLogger(ProveedorWhatsappLog.class);

    @Override
    public String enviar(String telefono, String mensaje) {
        String id = "LOG-" + UUID.randomUUID();
        log.info("[WhatsApp simulado] a {}: {} (id {})", telefono, mensaje, id);
        return id;
    }
}
