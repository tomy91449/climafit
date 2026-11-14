package ar.edu.utn.frc.climafit.clases.clase;

import ar.edu.utn.frc.climafit.clases.aviso.AvisoService;
import ar.edu.utn.frc.climafit.clases.aviso.TipoEvento;
import ar.edu.utn.frc.climafit.clases.client.actividades.ActividadDto;
import ar.edu.utn.frc.climafit.clases.client.actividades.ActividadesClient;
import ar.edu.utn.frc.climafit.clases.client.actividades.CompatibilidadDto;
import ar.edu.utn.frc.climafit.clases.client.actividades.EspacioDto;
import ar.edu.utn.frc.climafit.clases.estado.CambioEstado;
import ar.edu.utn.frc.climafit.clases.estado.CambioEstadoService;
import ar.edu.utn.frc.climafit.clases.estado.EstadoClase;
import ar.edu.utn.frc.climafit.clases.inscripcion.EstadoInscripcion;
import ar.edu.utn.frc.climafit.clases.inscripcion.Inscripcion;
import ar.edu.utn.frc.climafit.clases.inscripcion.InscripcionRepository;
import ar.edu.utn.frc.climafit.commons.error.NegocioException;
import ar.edu.utn.frc.climafit.commons.error.RecursoNoEncontradoException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Clases: programar, consultar, modificar, reprogramar manualmente y cancelar.
 *
 * Los cambios de estado se hacen dentro de una transacción (TransactionTemplate) y los avisos a ms-notificaciones
 * se mandan DESPUÉS de confirmarla: así una llamada HTTP lenta no retiene el bloqueo de la fila, y si el aviso
 * falla el cambio ya quedó guardado (RN16).
 */
@Service
public class ClaseService {

    /** Estados desde los que el profesor puede reprogramar a mano (propuesta, PATCH reprogramacion). */
    private static final Set<EstadoClase> REPROGRAMABLES =
            EnumSet.of(EstadoClase.PROGRAMADA, EstadoClase.REPROGRAMADA, EstadoClase.SUSPENDIDA_SIN_FECHA);

    private final ClaseRepository clases;
    private final InscripcionRepository inscripciones;
    private final CambioEstadoService estados;
    private final AvisoService avisos;
    private final ActividadesClient actividades;
    private final AutorizacionClase autorizacion;
    private final TransactionTemplate tx;
    private final Clock clock;

    public ClaseService(ClaseRepository clases, InscripcionRepository inscripciones, CambioEstadoService estados,
                        AvisoService avisos, ActividadesClient actividades, AutorizacionClase autorizacion,
                        TransactionTemplate tx, Clock clock) {
        this.clases = clases;
        this.inscripciones = inscripciones;
        this.estados = estados;
        this.avisos = avisos;
        this.actividades = actividades;
        this.autorizacion = autorizacion;
        this.tx = tx;
        this.clock = clock;
    }

    /** POST /clases: el profesorId sale del token. La clase nace PROGRAMADA y se registra su primer cambio_estado. */
    public ClaseResponse crear(ClaseRequest req, String profesorSub) {
        ActividadDto actividad = actividades.obtenerActividad(req.tipoActividadId());
        if (!actividad.estaActiva()) {
            throw new RecursoNoEncontradoException("La actividad " + req.tipoActividadId() + " no está vigente");
        }
        EspacioDto espacio = actividades.obtenerEspacio(req.espacioId()); // si no existe, 404
        if (!espacio.estaActivo()) {
            throw new RecursoNoEncontradoException("El espacio " + req.espacioId() + " no está vigente");
        }

        List<RutinaItemDto> rutina = req.rutina() == null ? List.of() : req.rutina();
        verificarRutina(req.tipoActividadId(), rutina); // RN07

        LocalDateTime fin = req.inicio().plusMinutes(req.duracionMinutos());
        if (clases.existeSuperposicionProfesor(profesorSub, req.inicio(), fin, null)) { // RN04
            throw new NegocioException("SUPERPOSICION_PROFESOR", HttpStatus.CONFLICT,
                    "El profesor ya tiene otra clase en ese horario");
        }

        return tx.execute(status -> {
            Clase clase = new Clase();
            clase.setTipoActividadId(req.tipoActividadId());
            clase.setEspacioId(req.espacioId());
            clase.setAlAireLibre(espacio.alAireLibre()); // copiado del espacio
            clase.setProfesorId(profesorSub);
            clase.setInicio(req.inicio());
            clase.setDuracionMin(req.duracionMinutos());
            clase.setCupo(req.cupo());
            clase.setEstado(EstadoClase.PROGRAMADA);
            clase.reemplazarRutina(aItems(rutina));
            Clase guardada = clases.save(clase);
            estados.registrarCreacion(guardada, profesorSub);
            return aRespuesta(guardada);
        });
    }

    public ClaseResponse obtener(Long id) {
        return aRespuesta(obtenerEntidad(id));
    }

    /**
     * GET /clases: "Buscar clases con cupo disponible y estado". Los filtros son opcionales; devuelve solo las
     * clases que todavía tienen lugares. desde y hasta aceptan fecha (2026-11-20) o fecha y hora
     * (2026-11-20T18:00:00); con solo fecha, hasta incluye todo ese día.
     */
    public List<ClaseResponse> buscar(Long actividadId, Long espacioId, String desde, String hasta,
                                      EstadoClase estado) {
        LocalDateTime desdeFecha = parsearFecha(desde, false, "desde");
        LocalDateTime hastaFecha = parsearFecha(hasta, true, "hasta");

        Specification<Clase> filtro = (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (actividadId != null) condiciones.add(cb.equal(root.get("tipoActividadId"), actividadId));
            if (espacioId != null) condiciones.add(cb.equal(root.get("espacioId"), espacioId));
            if (desdeFecha != null) condiciones.add(cb.greaterThanOrEqualTo(root.get("inicio"), desdeFecha));
            if (hastaFecha != null) condiciones.add(cb.lessThanOrEqualTo(root.get("inicio"), hastaFecha));
            if (estado != null) condiciones.add(cb.equal(root.get("estado"), estado));
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
        List<Clase> encontradas = clases.findAll(filtro, Sort.by("inicio", "id"));
        if (encontradas.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> ocupados = new HashMap<>();
        for (Object[] fila : inscripciones.contarPorClase(
                encontradas.stream().map(Clase::getId).toList(), EstadoInscripcion.ACTIVA)) {
            ocupados.put((Long) fila[0], (Long) fila[1]);
        }
        return encontradas.stream()
                .map(c -> ClaseResponse.de(c, c.getCupo() - ocupados.getOrDefault(c.getId(), 0L).intValue()))
                .filter(r -> r.lugaresDisponibles() > 0)
                .toList();
    }

    /** PUT /clases/{id}: modifica cupo, duración o rutina. Profesor dueño o ADMIN. */
    public ClaseResponse modificar(Long id, ClaseRequest req, String usuarioSub, boolean esAdmin) {
        Clase previa = obtenerEntidad(id);
        autorizacion.verificarDuenioOAdmin(previa, usuarioSub, esAdmin); // RN05
        estados.validarModificable(previa); // RN06
        if (req.rutina() != null) {
            verificarRutina(previa.getTipoActividadId(), req.rutina()); // RN07
        }
        if (req.duracionMinutos() != null) {
            LocalDateTime nuevoFin = previa.getInicio().plusMinutes(req.duracionMinutos());
            if (clases.existeSuperposicionProfesor(previa.getProfesorId(), previa.getInicio(), nuevoFin, id)) { // RN04
                throw new NegocioException("SUPERPOSICION_PROFESOR", HttpStatus.CONFLICT,
                        "Con la nueva duración la clase se superpone con otra del profesor");
            }
        }

        return tx.execute(status -> {
            // Se bloquea la clase para que el cupo no cambie mientras se inscribe alguien (RN01).
            Clase clase = clases.findByIdForUpdate(id)
                    .orElseThrow(() -> new RecursoNoEncontradoException("La clase " + id + " no existe"));
            estados.validarModificable(clase); // RN06
            if (req.cupo() != null) {
                long activos = inscripciones.countByClaseIdAndEstado(id, EstadoInscripcion.ACTIVA);
                if (req.cupo() < activos) {
                    throw new NegocioException("CUPO_COMPLETO", HttpStatus.CONFLICT,
                            "El nuevo cupo es menor que la cantidad de inscriptos (" + activos + ")");
                }
                clase.setCupo(req.cupo());
            }
            if (req.duracionMinutos() != null) {
                clase.setDuracionMin(req.duracionMinutos());
            }
            if (req.rutina() != null) {
                clase.reemplazarRutina(aItems(req.rutina()));
            }
            return aRespuesta(clases.save(clase));
        });
    }

    /** PATCH /clases/{id}/reprogramacion: el profesor dueño mueve la clase y se avisa a los inscriptos. */
    public ClaseResponse reprogramar(Long id, LocalDateTime nuevoInicio, String profesorSub) {
        Clase previa = obtenerEntidad(id);
        autorizacion.verificarDuenio(previa, profesorSub); // RN05
        estados.validarModificable(previa); // RN06
        if (!REPROGRAMABLES.contains(previa.getEstado())) {
            throw new NegocioException("CLASE_NO_MODIFICABLE", HttpStatus.CONFLICT,
                    "Una clase " + previa.getEstado() + " no se puede reprogramar");
        }
        if (!nuevoInicio.isAfter(LocalDateTime.now(clock))) {
            throw new NegocioException("DATOS_INVALIDOS", HttpStatus.BAD_REQUEST,
                    "nuevoInicio debe ser una fecha futura");
        }
        LocalDateTime nuevoFin = nuevoInicio.plusMinutes(previa.getDuracionMin());
        if (clases.existeSuperposicionProfesor(previa.getProfesorId(), nuevoInicio, nuevoFin, id)) { // RN04
            throw new NegocioException("SUPERPOSICION_PROFESOR", HttpStatus.CONFLICT,
                    "El profesor ya tiene otra clase en ese horario");
        }

        CambioEstado cambio = tx.execute(status -> {
            Clase clase = clases.findById(id)
                    .orElseThrow(() -> new RecursoNoEncontradoException("La clase " + id + " no existe"));
            return estados.cambiarEstado(clase, EstadoClase.REPROGRAMADA, "Reprogramada por el profesor",
                    profesorSub, nuevoInicio);
        });

        List<String> destinatarios = alumnosActivos(id);
        avisos.avisar(cambio, TipoEvento.REPROGRAMACION, null, destinatarios); // fuera de la transacción (RN16)
        return obtener(id);
    }

    /** PATCH /clases/{id}/cancelacion: cancela la clase y sus inscripciones y avisa. Profesor dueño o ADMIN. */
    public ClaseResponse cancelar(Long id, String motivo, String usuarioSub, boolean esAdmin) {
        Clase previa = obtenerEntidad(id);
        autorizacion.verificarDuenioOAdmin(previa, usuarioSub, esAdmin); // RN05
        estados.validarModificable(previa); // RN06

        ResultadoCancelacion resultado = tx.execute(status -> {
            // Se bloquea la clase: nadie se inscribe mientras se cancelan las inscripciones.
            Clase clase = clases.findByIdForUpdate(id)
                    .orElseThrow(() -> new RecursoNoEncontradoException("La clase " + id + " no existe"));
            List<Inscripcion> activas = inscripciones.findByClaseIdAndEstado(id, EstadoInscripcion.ACTIVA);
            CambioEstado cambio = estados.cambiarEstado(clase, EstadoClase.CANCELADA, motivo, usuarioSub);
            activas.forEach(i -> i.setEstado(EstadoInscripcion.CANCELADA));
            inscripciones.saveAll(activas);
            return new ResultadoCancelacion(cambio, activas.stream().map(Inscripcion::getAlumnoId).toList());
        });

        avisos.avisar(resultado.cambio(), TipoEvento.CANCELACION, null, resultado.alumnos()); // RN16
        return obtener(id);
    }

    public Clase obtenerEntidad(Long id) {
        return clases.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La clase " + id + " no existe"));
    }

    /** RN07: todos los ejercicios de la rutina tienen que servir para la actividad (422 EJERCICIO_INCOMPATIBLE). */
    private void verificarRutina(Long actividadId, List<RutinaItemDto> rutina) {
        if (rutina == null || rutina.isEmpty()) {
            return;
        }
        List<Long> ids = rutina.stream().map(RutinaItemDto::ejercicioId).distinct().toList();
        CompatibilidadDto resultado = actividades.verificarCompatibilidad(actividadId, ids);
        if (!resultado.compatible()) {
            throw new NegocioException("EJERCICIO_INCOMPATIBLE", HttpStatus.UNPROCESSABLE_ENTITY,
                    "Ejercicios incompatibles con la actividad: " + resultado.incompatibles());
        }
    }

    private List<String> alumnosActivos(Long claseId) {
        return inscripciones.findByClaseIdAndEstado(claseId, EstadoInscripcion.ACTIVA).stream()
                .map(Inscripcion::getAlumnoId)
                .toList();
    }

    private List<RutinaItem> aItems(List<RutinaItemDto> dtos) {
        List<RutinaItem> items = new ArrayList<>();
        for (RutinaItemDto dto : dtos) {
            RutinaItem item = new RutinaItem();
            item.setEjercicioId(dto.ejercicioId());
            item.setOrden(dto.orden());
            item.setIndicaciones(dto.indicaciones());
            items.add(item);
        }
        return items;
    }

    private ClaseResponse aRespuesta(Clase clase) {
        long ocupados = inscripciones.countByClaseIdAndEstado(clase.getId(), EstadoInscripcion.ACTIVA);
        return ClaseResponse.de(clase, clase.getCupo() - (int) ocupados);
    }

    private LocalDateTime parsearFecha(String valor, boolean finDelDia, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            if (valor.length() <= 10) {
                LocalDate dia = LocalDate.parse(valor);
                return finDelDia ? dia.atTime(LocalTime.of(23, 59, 59)) : dia.atStartOfDay();
            }
            return LocalDateTime.parse(valor);
        } catch (DateTimeParseException e) {
            throw new NegocioException("DATOS_INVALIDOS", HttpStatus.BAD_REQUEST,
                    "Fecha inválida en '" + campo + "': " + valor);
        }
    }

    private record ResultadoCancelacion(CambioEstado cambio, List<String> alumnos) {
    }
}
