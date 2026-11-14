package ar.edu.utn.frc.climafit.actividades.controller;

import ar.edu.utn.frc.climafit.actividades.dto.EspacioRequest;
import ar.edu.utn.frc.climafit.actividades.dto.EspacioResponse;
import ar.edu.utn.frc.climafit.actividades.service.EspacioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

/** Endpoints de espacios (propuesta, sección 07). */
@RestController
@RequestMapping("/espacios")
@Tag(name = "Espacios", description = "Espacios con coordenadas, al aire libre o techados (F03)")
public class EspacioController {

    private final EspacioService servicio;

    public EspacioController(EspacioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Listar los espacios disponibles")
    public List<EspacioResponse> listar() {
        return servicio.listarDisponibles();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un espacio con sus coordenadas y si es al aire libre")
    public EspacioResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registrar un espacio con coordenadas")
    public ResponseEntity<EspacioResponse> crear(@Valid @RequestBody EspacioRequest pedido) {
        EspacioResponse creado = servicio.crear(pedido);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar los datos de un espacio")
    public EspacioResponse actualizar(@PathVariable Long id, @Valid @RequestBody EspacioRequest pedido) {
        return servicio.actualizar(id, pedido);
    }
}
