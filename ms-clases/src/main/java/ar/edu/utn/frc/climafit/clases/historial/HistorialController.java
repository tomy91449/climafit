package ar.edu.utn.frc.climafit.clases.historial;

import ar.edu.utn.frc.climafit.commons.seguridad.UsuarioActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Historial")
public class HistorialController {

    private final HistorialService servicio;
    private final UsuarioActual usuario;

    public HistorialController(HistorialService servicio, UsuarioActual usuario) {
        this.servicio = servicio;
        this.usuario = usuario;
    }

    @GetMapping("/clases/{id}/historial")
    @PreAuthorize("hasAnyRole('PROFESOR','ADMIN')")
    @Operation(summary = "Consultar los cambios de estado de una clase")
    public List<HistorialItem> historial(@PathVariable Long id) {
        return servicio.consultar(id, usuario.sub(), usuario.tieneRol("ADMIN"));
    }
}
