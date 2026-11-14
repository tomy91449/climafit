package ar.edu.utn.frc.climafit.notificaciones.controlador;

import ar.edu.utn.frc.climafit.notificaciones.dto.EventoNotificacionRequest;
import ar.edu.utn.frc.climafit.notificaciones.dto.EventoNotificacionResponse;
import ar.edu.utn.frc.climafit.notificaciones.servicio.NotificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Trusted zone: solo lo llama ms-clases, con el token de la service account (rol SERVICIO).
 * El gateway no tiene ruta hacia /internal/**, así que desde afuera da 404.
 */
@RestController
@RequestMapping("/internal/notificaciones")
@Tag(name = "Interno", description = "Uso exclusivo de otros microservicios (token de servicio)")
public class NotificacionInternaController {

    private final NotificacionService servicio;

    public NotificacionInternaController(NotificacionService servicio) {
        this.servicio = servicio;
    }

    /**
     * 202 Accepted (y no 201): el pedido quedó registrado pero los mensajes todavía no se mandaron;
     * eso ocurre en segundo plano. ms-clases no espera a WhatsApp.
     */
    @PostMapping
    @PreAuthorize("hasRole('SERVICIO')")
    @Operation(summary = "Registrar un evento de clase y programar el envío a cada destinatario (RN14, RN15)")
    public ResponseEntity<EventoNotificacionResponse> registrar(@Valid @RequestBody EventoNotificacionRequest request) {
        EventoNotificacionResponse respuesta;
        try {
            respuesta = servicio.registrarEvento(request);
        } catch (DataIntegrityViolationException ex) {
            // Dos pedidos iguales llegaron AL MISMO TIEMPO: los dos vieron que no había nada y uno chocó
            // con la restricción única (evento, usuario). Al repetirlo, ya ve lo que guardó el otro y
            // lo ignora como duplicado (RN15), sin devolverle un error a ms-clases.
            respuesta = servicio.registrarEvento(request);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(respuesta);
    }
}
