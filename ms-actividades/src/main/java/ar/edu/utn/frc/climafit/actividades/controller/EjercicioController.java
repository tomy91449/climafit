package ar.edu.utn.frc.climafit.actividades.controller;

import ar.edu.utn.frc.climafit.actividades.dto.CompatibilidadRequest;
import ar.edu.utn.frc.climafit.actividades.dto.CompatibilidadResponse;
import ar.edu.utn.frc.climafit.actividades.dto.EjercicioRequest;
import ar.edu.utn.frc.climafit.actividades.dto.EjercicioResponse;
import ar.edu.utn.frc.climafit.actividades.service.EjercicioService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/** Endpoints de ejercicios (propuesta, sección 07). */
@RestController
@RequestMapping("/ejercicios")
@Tag(name = "Ejercicios", description = "Ejercicios base y personalizados (F04, RN07, RN08)")
public class EjercicioController {

    private final EjercicioService servicio;

    public EjercicioController(EjercicioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Consultar los ejercicios visibles (base y propios) compatibles con una actividad")
    public List<EjercicioResponse> listar(@RequestParam(required = false) Long actividadId) {
        return servicio.listarVisibles(actividadId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar la descripción de un ejercicio")
    public EjercicioResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESOR')")
    @Operation(summary = "Registrar un ejercicio base (admin) o personalizado (profesor)")
    public ResponseEntity<EjercicioResponse> crear(@Valid @RequestBody EjercicioRequest pedido) {
        EjercicioResponse creado = servicio.crear(pedido);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESOR')")
    @Operation(summary = "Modificar un ejercicio")
    public EjercicioResponse actualizar(@PathVariable Long id, @Valid @RequestBody EjercicioRequest pedido) {
        return servicio.actualizar(id, pedido);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESOR')")
    @Operation(summary = "Dar de baja un ejercicio sin afectar rutinas existentes")
    public ResponseEntity<Void> darDeBaja(@PathVariable Long id) {
        servicio.darDeBaja(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/compatibilidad")
    @PreAuthorize("hasAnyRole('PROFESOR', 'SERVICIO')")
    @Operation(summary = "Verificar que un conjunto de ejercicios es compatible con una actividad (RN07)")
    public CompatibilidadResponse verificarCompatibilidad(@Valid @RequestBody CompatibilidadRequest pedido) {
        return servicio.verificarCompatibilidad(pedido);
    }
}
