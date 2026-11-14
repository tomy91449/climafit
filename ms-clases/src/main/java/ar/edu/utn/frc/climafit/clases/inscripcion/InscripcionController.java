package ar.edu.utn.frc.climafit.clases.inscripcion;

import ar.edu.utn.frc.climafit.commons.seguridad.UsuarioActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Inscripciones")
public class InscripcionController {

    private final InscripcionService servicio;
    private final UsuarioActual usuario;

    public InscripcionController(InscripcionService servicio, UsuarioActual usuario) {
        this.servicio = servicio;
        this.usuario = usuario;
    }

    @PostMapping("/clases/{id}/inscripciones")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ALUMNO')")
    @Operation(summary = "Inscribirse a una clase")
    public InscripcionResponse inscribir(@PathVariable Long id) {
        return servicio.inscribir(id, usuario.sub());
    }

    @GetMapping("/clases/{id}/inscripciones")
    @PreAuthorize("hasAnyRole('PROFESOR','ADMIN')")
    @Operation(summary = "Listar los inscriptos de una clase")
    public List<InscripcionResponse> listarInscriptos(@PathVariable Long id) {
        return servicio.listarInscriptos(id, usuario.sub(), usuario.tieneRol("ADMIN"));
    }

    @GetMapping("/inscripciones/mias")
    @PreAuthorize("hasRole('ALUMNO')")
    @Operation(summary = "Consultar las inscripciones propias")
    public List<InscripcionResponse> mias() {
        return servicio.listarMias(usuario.sub());
    }

    @PatchMapping("/inscripciones/{id}/cancelacion")
    @PreAuthorize("hasRole('ALUMNO')")
    @Operation(summary = "Cancelar una inscripción propia y liberar el lugar")
    public InscripcionResponse cancelar(@PathVariable Long id) {
        return servicio.cancelar(id, usuario.sub());
    }
}
