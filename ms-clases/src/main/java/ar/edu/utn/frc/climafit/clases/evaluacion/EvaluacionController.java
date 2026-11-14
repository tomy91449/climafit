package ar.edu.utn.frc.climafit.clases.evaluacion;

import ar.edu.utn.frc.climafit.clases.clase.Clase;
import ar.edu.utn.frc.climafit.clases.clase.ClaseRepository;
import ar.edu.utn.frc.climafit.commons.error.RecursoNoEncontradoException;
import ar.edu.utn.frc.climafit.commons.seguridad.UsuarioActual;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de evaluación climática de ms-clases (tarea 4.8). */
@RestController
@RequestMapping("/clases")
public class EvaluacionController {

    /** Cuerpo de GET /clases/{id}/evaluaciones: una fila de evaluacion_climatica. */
    public record EvaluacionRegistradaDto(LocalDateTime fechaEvaluacion, LocalDateTime inicioEvaluado,
                                          ResultadoEvaluacion resultado, String motivo, Integer probLluvia,
                                          Integer vientoKmh, Integer temperaturaC, Boolean tormenta,
                                          Double precipitacionMm) {
        static EvaluacionRegistradaDto de(EvaluacionClimatica e) {
            return new EvaluacionRegistradaDto(e.getFechaEvaluacion(), e.getInicioEvaluado(), e.getResultado(),
                    e.getMotivo(), e.getProbLluvia(), e.getVientoKmh(), e.getTemperaturaC(), e.getTormenta(),
                    e.getPrecipitacionMm());
        }
    }

    private final ClaseRepository clases;
    private final EvaluacionService evaluacion;
    private final EvaluacionGeneralService general;
    private final EvaluacionClimaticaRepository evaluaciones;
    private final AutorizacionClase autorizacion;
    private final UsuarioActual usuario;

    public EvaluacionController(ClaseRepository clases, EvaluacionService evaluacion,
                                EvaluacionGeneralService general, EvaluacionClimaticaRepository evaluaciones,
                                AutorizacionClase autorizacion, UsuarioActual usuario) {
        this.clases = clases;
        this.evaluacion = evaluacion;
        this.general = general;
        this.evaluaciones = evaluaciones;
        this.autorizacion = autorizacion;
        this.usuario = usuario;
    }

    /** Evalúa el clima de una clase y aplica la decisión. Siempre 200, también si es FALLIDA. */
    @PostMapping("/{id}/evaluaciones")
    @PreAuthorize("hasAnyRole('PROFESOR','ADMIN')")
    public EvaluacionRespuesta evaluar(@PathVariable Long id) {
        Clase clase = buscar(id);
        autorizacion.verificarProfesorDuenoOAdmin(clase);   // RN05
        return evaluacion.evaluarManual(clase, usuario.id());   // RN09 -> 409 CLASE_NO_EVALUABLE
    }

    @GetMapping("/{id}/evaluaciones")
    @PreAuthorize("hasAnyRole('PROFESOR','ADMIN')")
    public List<EvaluacionRegistradaDto> listar(@PathVariable Long id) {
        autorizacion.verificarProfesorDuenoOAdmin(buscar(id));   // RN05
        return evaluaciones.findByClaseIdOrderByFechaEvaluacionDesc(id).stream()
                .map(EvaluacionRegistradaDto::de).toList();
    }

    /** Evaluación general de las próximas 24 h (misma lógica que la tarea programada). */
    @PostMapping("/evaluaciones")
    @PreAuthorize("hasRole('ADMIN')")
    public ResumenEvaluacionGeneral evaluarTodas() {
        return general.ejecutar(usuario.id());
    }

    private Clase buscar(Long id) {
        return clases.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("No existe la clase " + id));
    }
}
