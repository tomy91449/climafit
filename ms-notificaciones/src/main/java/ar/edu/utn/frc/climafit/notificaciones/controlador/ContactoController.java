package ar.edu.utn.frc.climafit.notificaciones.controlador;

import ar.edu.utn.frc.climafit.notificaciones.dto.ContactoRequest;
import ar.edu.utn.frc.climafit.notificaciones.dto.ContactoResponse;
import ar.edu.utn.frc.climafit.notificaciones.servicio.ContactoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * F10. Cualquier usuario autenticado gestiona su propio contacto (alumnos para recibir avisos,
 * profesores para recibir las alertas de riesgo). El gateway recibe /api/contactos y reenvía /contactos.
 */
@RestController
@RequestMapping("/contactos")
@Tag(name = "Contactos", description = "Chat de Telegram y consentimiento para avisos (F10, RN14)")
public class ContactoController {

    private final ContactoService servicio;

    public ContactoController(ContactoService servicio) {
        this.servicio = servicio;
    }

    @PutMapping("/mio")
    @Operation(summary = "Aceptar o rechazar los avisos por Telegram",
            description = "Con {\"aceptaTelegram\": true} la respuesta trae linkVinculacion: el usuario lo abre, "
                    + "toca Iniciar en el bot y su chat queda registrado solo (vinculado pasa a true). "
                    + "telegramChatId es opcional y permite cargar el chat a mano.")
    public ContactoResponse guardar(@Valid @RequestBody ContactoRequest request) {
        return servicio.guardarMio(request);
    }

    @GetMapping("/mio")
    @Operation(summary = "Consultar las preferencias de contacto propias")
    public ContactoResponse obtener() {
        return servicio.obtenerMio();
    }

    @PostMapping("/mio/vinculacion")
    @Operation(summary = "Pedir un link nuevo para vincular o cambiar la cuenta de Telegram",
            description = "Sirve aunque ya tenga un chat vinculado (por ejemplo, para pasar a otra cuenta). "
                    + "El chat actual se sigue usando hasta que se toca Iniciar desde el link nuevo. "
                    + "404 si nunca guardó sus preferencias, 409 si no aceptó los avisos y 503 si el bot "
                    + "de Telegram no está disponible (o el proveedor es \"log\").")
    public ContactoResponse nuevaVinculacion() {
        return servicio.nuevaVinculacion();
    }
}
