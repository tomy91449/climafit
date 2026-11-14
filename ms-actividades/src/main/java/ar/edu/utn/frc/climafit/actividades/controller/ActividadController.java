package ar.edu.utn.frc.climafit.actividades.controller;

import ar.edu.utn.frc.climafit.actividades.dto.ActividadRequest;
import ar.edu.utn.frc.climafit.actividades.dto.ActividadResponse;
import ar.edu.utn.frc.climafit.actividades.service.ActividadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/** Endpoints de tipos de actividad (propuesta, sección 07). */
@RestController
@RequestMapping("/actividades")
@Tag(name = "Tipos de actividad", description = "Tipos de actividad y sus criterios climáticos (F02)")
public class ActividadController {

    private final ActividadService servicio;

    public ActividadController(ActividadService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Listar los tipos de actividad vigentes con sus criterios climáticos")
    public List<ActividadResponse> listar() {
        return servicio.listarVigentes();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un tipo de actividad y sus criterios climáticos")
    public ActividadResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registrar un tipo de actividad con sus criterios climáticos")
    public ResponseEntity<ActividadResponse> crear(@Valid @RequestBody ActividadRequest pedido) {
        ActividadResponse creada = servicio.crear(pedido);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(creada.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar descripción y criterios climáticos de una actividad")
    public ActividadResponse actualizar(@PathVariable Long id, @Valid @RequestBody ActividadRequest pedido) {
        return servicio.actualizar(id, pedido);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Dar de baja una actividad para que no se use en nuevas clases")
    public ResponseEntity<Void> darDeBaja(@PathVariable Long id) {
        servicio.darDeBaja(id);
        return ResponseEntity.noContent().build();
    }
}
