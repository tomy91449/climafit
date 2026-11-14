package ar.edu.utn.frc.climafit.clases.clase;

import ar.edu.utn.frc.climafit.clases.estado.EstadoClase;
import ar.edu.utn.frc.climafit.commons.seguridad.UsuarioActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/clases")
@Tag(name = "Clases")
public class ClaseController {

    private final ClaseService servicio;
    private final UsuarioActual usuario;

    public ClaseController(ClaseService servicio, UsuarioActual usuario) {
        this.servicio = servicio;
        this.usuario = usuario;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PROFESOR')")
    @Operation(summary = "Programar una clase con su rutina")
    public ClaseResponse crear(@Validated(Crear.class) @RequestBody ClaseRequest cuerpo) {
        return servicio.crear(cuerpo, usuario.sub());
    }

    @GetMapping
    @Operation(summary = "Buscar clases con cupo disponible y estado")
    public List<ClaseResponse> buscar(@RequestParam(required = false) Long actividadId,
                                      @RequestParam(required = false) Long espacioId,
                                      @RequestParam(required = false) String desde,
                                      @RequestParam(required = false) String hasta,
                                      @RequestParam(required = false) EstadoClase estado) {
        return servicio.buscar(actividadId, espacioId, desde, hasta, estado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar una clase con su rutina, cupo y estado")
    public ClaseResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROFESOR','ADMIN')")
    @Operation(summary = "Modificar cupo, duración o rutina de una clase")
    public ClaseResponse modificar(@PathVariable Long id, @Valid @RequestBody ClaseRequest cuerpo) {
        return servicio.modificar(id, cuerpo, usuario.sub(), usuario.tieneRol("ADMIN"));
    }

    @PatchMapping("/{id}/reprogramacion")
    @PreAuthorize("hasRole('PROFESOR')")
    @Operation(summary = "Reprogramar manualmente una clase y notificar a los inscriptos")
    public ClaseResponse reprogramar(@PathVariable Long id, @Valid @RequestBody ReprogramacionRequest cuerpo) {
        return servicio.reprogramar(id, cuerpo.nuevoInicio(), usuario.sub());
    }

    @PatchMapping("/{id}/cancelacion")
    @PreAuthorize("hasAnyRole('PROFESOR','ADMIN')")
    @Operation(summary = "Cancelar una clase, cancelar inscripciones y notificar")
    public ClaseResponse cancelar(@PathVariable Long id, @Valid @RequestBody CancelacionRequest cuerpo) {
        return servicio.cancelar(id, cuerpo.motivo(), usuario.sub(), usuario.tieneRol("ADMIN"));
    }
}
