package ar.edu.utn.frc.climafit.clases.inscripcion;

import ar.edu.utn.frc.climafit.clases.clase.AutorizacionClase;
import ar.edu.utn.frc.climafit.clases.clase.Clase;
import ar.edu.utn.frc.climafit.clases.clase.ClaseRepository;
import ar.edu.utn.frc.climafit.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.climafit.commons.error.NegocioException;
import ar.edu.utn.frc.climafit.commons.error.RecursoNoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class InscripcionService {

    private final InscripcionRepository inscripciones;
    private final ClaseRepository clases;
    private final AutorizacionClase autorizacion;
    private final Clock clock;

    public InscripcionService(InscripcionRepository inscripciones, ClaseRepository clases,
                              AutorizacionClase autorizacion, Clock clock) {
        this.inscripciones = inscripciones;
        this.clases = clases;
        this.autorizacion = autorizacion;
        this.clock = clock;
    }

    /**
     * POST /clases/{id}/inscripciones.
     *
     * RN01: se bloquea la fila de la clase con SELECT ... FOR UPDATE antes de contar las inscripciones ACTIVA.
     * Si dos alumnos van por el último lugar a la vez, el segundo espera a que termine el primero y recién
     * entonces cuenta, así que el cupo nunca se pasa.
     */
    @Transactional
    public InscripcionResponse inscribir(Long claseId, String alumnoSub) {
        Clase clase = clases.findByIdForUpdate(claseId) // RN01: bloqueo
                .orElseThrow(() -> new RecursoNoEncontradoException("La clase " + claseId + " no existe"));
        LocalDateTime ahora = LocalDateTime.now(clock);

        // RN02: solo clases PROGRAMADA, EN_RIESGO o REPROGRAMADA que no empezaron
        if (!clase.getEstado().esVigente() || !clase.getInicio().isAfter(ahora)) {
            throw new NegocioException(HttpStatus.CONFLICT, "CLASE_NO_DISPONIBLE",
                    "La clase no admite inscripciones");
        }
        // RN02: no dos veces a la misma clase
        Inscripcion previa = inscripciones.findByClaseIdAndAlumnoId(claseId, alumnoSub).orElse(null);
        if (previa != null && previa.getEstado() == EstadoInscripcion.ACTIVA) {
            throw new NegocioException(HttpStatus.CONFLICT, "YA_INSCRIPTO", "El alumno ya está inscripto en la clase");
        }
        // RN03: el alumno no puede tener otra inscripción activa que se superponga en horario
        if (clases.existeSuperposicionAlumno(alumnoSub, clase.getInicio(), clase.getFin(), claseId)) {
            throw new NegocioException(HttpStatus.CONFLICT, "SUPERPOSICION_ALUMNO",
                    "El alumno ya tiene otra clase en ese horario");
        }
        // RN01: cupo
        long activas = inscripciones.countByClaseIdAndEstado(claseId, EstadoInscripcion.ACTIVA);
        if (activas >= clase.getCupo()) {
            throw new NegocioException(HttpStatus.CONFLICT, "CUPO_COMPLETO", "La clase no tiene lugares disponibles");
        }

        // Si el alumno había cancelado antes, se reactiva su misma fila (la restricción única
        // (clase_id, alumno_id) no deja crear una segunda).
        Inscripcion inscripcion = previa != null ? previa : new Inscripcion();
        inscripcion.setClaseId(claseId);
        inscripcion.setAlumnoId(alumnoSub);
        inscripcion.setEstado(EstadoInscripcion.ACTIVA);
        inscripcion.setFechaAlta(ahora.withNano(0));
        return InscripcionResponse.de(inscripciones.save(inscripcion));
    }

    /** GET /clases/{id}/inscripciones: los inscriptos activos. Profesor dueño o ADMIN. */
    @Transactional(readOnly = true)
    public List<InscripcionResponse> listarInscriptos(Long claseId, String usuarioSub, boolean esAdmin) {
        Clase clase = clases.findById(claseId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La clase " + claseId + " no existe"));
        autorizacion.verificarDuenioOAdmin(clase, usuarioSub, esAdmin); // RN05
        return inscripciones.findByClaseIdAndEstado(claseId, EstadoInscripcion.ACTIVA).stream()
                .map(InscripcionResponse::de)
                .toList();
    }

    /** GET /inscripciones/mias. */
    @Transactional(readOnly = true)
    public List<InscripcionResponse> listarMias(String alumnoSub) {
        return inscripciones.findByAlumnoIdOrderByFechaAltaDesc(alumnoSub).stream()
                .map(InscripcionResponse::de)
                .toList();
    }

    /** PATCH /inscripciones/{id}/cancelacion: solo el alumno dueño, solo antes del inicio, y libera el lugar. */
    @Transactional
    public InscripcionResponse cancelar(Long inscripcionId, String alumnoSub) {
        Inscripcion inscripcion = inscripciones.findById(inscripcionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La inscripción " + inscripcionId + " no existe"));
        if (!inscripcion.getAlumnoId().equals(alumnoSub)) {
            throw new AccesoDenegadoException("La inscripción pertenece a otro alumno");
        }
        if (inscripcion.getEstado() == EstadoInscripcion.CANCELADA) {
            return InscripcionResponse.de(inscripcion); // ya estaba cancelada
        }
        Clase clase = clases.findById(inscripcion.getClaseId())
                .orElseThrow(() -> new RecursoNoEncontradoException("La clase de la inscripción no existe"));
        // RN02: la cancelación solo antes del inicio
        if (!clase.getInicio().isAfter(LocalDateTime.now(clock))) {
            throw new NegocioException(HttpStatus.CONFLICT, "CLASE_NO_DISPONIBLE",
                    "La clase ya comenzó: no se puede cancelar la inscripción");
        }
        inscripcion.setEstado(EstadoInscripcion.CANCELADA);
        return InscripcionResponse.de(inscripciones.save(inscripcion));
    }
}
