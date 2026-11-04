package ar.edu.utn.frc.climafit.actividades.controlador;

import ar.edu.utn.frc.climafit.actividades.dto.TipoActividadRequest;
import ar.edu.utn.frc.climafit.actividades.dto.TipoActividadResponse;
import ar.edu.utn.frc.climafit.actividades.servicio.TipoActividadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Las rutas NO llevan "/api": el gateway recibe /api/actividades y reenvía /actividades.
 * Cualquier usuario autenticado puede leer; solo ADMIN registra (F02).
 */
@RestController
@RequestMapping("/actividades")
@Tag(name = "Tipos de actividad", description = "Actividades y sus criterios climáticos (F02)")
public class TipoActividadController {

    private final TipoActividadService servicio;

    public TipoActividadController(TipoActividadService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Listar los tipos de actividad vigentes con sus criterios climáticos")
    public List<TipoActividadResponse> listar() {
        return servicio.listarVigentes();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un tipo de actividad (también lo usa ms-clases con token de servicio)")
    public TipoActividadResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registrar un tipo de actividad con sus criterios climáticos")
    public ResponseEntity<TipoActividadResponse> registrar(@Valid @RequestBody TipoActividadRequest request) {
        TipoActividadResponse creada = servicio.registrar(request);
        // 201 Created + header Location con la URL del recurso nuevo
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creada.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }
}
