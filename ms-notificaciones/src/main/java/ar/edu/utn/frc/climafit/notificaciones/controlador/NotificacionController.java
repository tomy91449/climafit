package ar.edu.utn.frc.climafit.notificaciones.controlador;

import ar.edu.utn.frc.climafit.notificaciones.dto.NotificacionResponse;
import ar.edu.utn.frc.climafit.notificaciones.servicio.NotificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Consulta de las notificaciones de una clase. El rol se controla acá (y antes en el gateway);
 * que el profesor sea el DUEÑO de la clase se controla en el servicio (RN05).
 */
@RestController
@RequestMapping("/notificaciones")
@Validated
@Tag(name = "Notificaciones", description = "Estado de los avisos por Telegram de cada clase (F14)")
public class NotificacionController {

    private final NotificacionService servicio;

    public NotificacionController(NotificacionService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESOR')")
    @Operation(summary = "Consultar las notificaciones generadas para una clase y su estado")
    public List<NotificacionResponse> listar(@RequestParam @Positive Long claseId) {
        return servicio.listarPorClase(claseId);
    }
}
