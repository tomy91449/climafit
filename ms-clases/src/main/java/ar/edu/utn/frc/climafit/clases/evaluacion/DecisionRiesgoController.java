package ar.edu.utn.frc.climafit.clases.evaluacion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ar.edu.utn.frc.climafit.clases.clase.Clase;
import ar.edu.utn.frc.climafit.clases.clase.ClaseRepository;
import ar.edu.utn.frc.climafit.clases.clase.ClaseRespuesta;
import ar.edu.utn.frc.climafit.clases.inscripcion.EstadoInscripcion;
import ar.edu.utn.frc.climafit.clases.inscripcion.InscripcionRepository;
import ar.edu.utn.frc.climafit.commons.error.RecursoNoEncontradoException;
import ar.edu.utn.frc.climafit.commons.seguridad.UsuarioActual;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** PATCH /clases/{id}/decision-riesgo (RN05, RN12). Seguridad: profesor dueño. */
@Tag(name = "Clases en riesgo", description = "Decisión del profesor sobre una clase EN_RIESGO (RN12)")
@RestController
@RequestMapping("/clases")
public class DecisionRiesgoController {

    public record DecisionRequest(@NotNull DecisionRiesgo decision) {
    }

    private final ClaseRepository clases;
    private final InscripcionRepository inscripciones;
    private final DecisionRiesgoService decisiones;
    private final AutorizacionClase autorizacion;
    private final UsuarioActual usuario;

    public DecisionRiesgoController(ClaseRepository clases, InscripcionRepository inscripciones,
                                    DecisionRiesgoService decisiones, AutorizacionClase autorizacion,
                                    UsuarioActual usuario) {
        this.clases = clases;
        this.inscripciones = inscripciones;
        this.decisiones = decisiones;
        this.autorizacion = autorizacion;
        this.usuario = usuario;
    }

    @Operation(summary = "Mantener o suspender una clase EN_RIESGO (solo el profesor dueño)")
    @PatchMapping("/{id}/decision-riesgo")
    @PreAuthorize("hasRole('PROFESOR')")
    public ClaseRespuesta decidir(@PathVariable Long id, @Valid @RequestBody DecisionRequest pedido) {
        Clase clase = clases.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la clase " + id));
        autorizacion.verificarProfesorDueno(clase);   // RN05 (antes que el estado: profesor ajeno -> 403)
        Clase actualizada = decisiones.decidir(clase, pedido.decision(), usuario.id());
        int activos = inscripciones.findByClaseIdAndEstado(id, EstadoInscripcion.ACTIVA).size();
        return ClaseRespuesta.de(actualizada, activos);
    }
}
