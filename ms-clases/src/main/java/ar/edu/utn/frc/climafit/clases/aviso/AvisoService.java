package ar.edu.utn.frc.climafit.clases.aviso;

import ar.edu.utn.frc.climafit.clases.clase.Clase;
import ar.edu.utn.frc.climafit.clases.clase.ClaseRepository;
import ar.edu.utn.frc.climafit.clases.client.actividades.ActividadesClient;
import ar.edu.utn.frc.climafit.clases.estado.CambioEstado;
import ar.edu.utn.frc.climafit.clases.estado.CambioEstadoRepository;
import ar.edu.utn.frc.climafit.clases.inscripcion.EstadoInscripcion;
import ar.edu.utn.frc.climafit.clases.inscripcion.Inscripcion;
import ar.edu.utn.frc.climafit.clases.inscripcion.InscripcionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Arma los avisos y los manda a ms-notificaciones.
 * RN16: si ms-notificaciones falla, el cambio de estado NO se deshace; el aviso queda pendiente
 * (notificado = false) y se reenvía después con reenviarPendientes().
 */
@Service
public class AvisoService {

    private static final Logger log = LoggerFactory.getLogger(AvisoService.class);
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final NotificacionesClient notificaciones;
    private final CambioEstadoRepository cambios;
    private final ClaseRepository clases;
    private final InscripcionRepository inscripciones;
    private final ActividadesClient actividades;

    public AvisoService(NotificacionesClient notificaciones, CambioEstadoRepository cambios, ClaseRepository clases,
                        InscripcionRepository inscripciones, ActividadesClient actividades) {
        this.notificaciones = notificaciones;
        this.cambios = cambios;
        this.clases = clases;
        this.inscripciones = inscripciones;
        this.actividades = actividades;
    }

    /**
     * Firma de la Fase 0 (punto 9): arma el evento, llama a ms-notificaciones y marca notificado.
     *
     * @param mensaje texto del aviso; si es null o está en blanco, se arma en español con la actividad, la fecha,
     *                el motivo y la nueva fecha (si la hay)
     */
    public void avisar(CambioEstado cambio, TipoEvento tipo, String mensaje, List<String> destinatarios) {
        if (destinatarios == null || destinatarios.isEmpty()) {
            marcar(cambio, true); // no hay a quién avisar: nada queda pendiente
            return;
        }
        Clase clase = clases.findById(cambio.getClaseId()).orElse(null);
        if (clase == null) {
            marcar(cambio, true);
            return;
        }
        String texto = (mensaje == null || mensaje.isBlank()) ? construirMensaje(tipo, clase, cambio) : mensaje;
        EventoNotificacion evento = new EventoNotificacion(eventoId(cambio), tipo, clase.getId(),
                clase.getProfesorId(), texto, List.copyOf(destinatarios));
        boolean entregado;
        try {
            notificaciones.enviar(evento);
            entregado = true;
        } catch (RuntimeException e) {
            // RN16: se registra y se deja pendiente; el estado de la clase no se toca.
            log.warn("No se pudo avisar el evento {}: {}", evento.eventoId(), e.getMessage());
            entregado = false;
        }
        marcar(cambio, entregado);
    }

    /**
     * RN16: reenvía los cambios con notificado = false, con el mismo eventoId (RN15 en ms-notificaciones
     * evita los duplicados). Lo llama la tarea programada de la Persona 4.
     *
     * @return cuántos avisos se entregaron en esta pasada
     */
    public int reenviarPendientes() {
        int entregados = 0;
        for (CambioEstado cambio : cambios.findByNotificadoFalseOrderByIdAsc()) {
            TipoEvento tipo = tipoPara(cambio);
            Clase clase = clases.findById(cambio.getClaseId()).orElse(null);
            if (tipo == null || clase == null) {
                marcar(cambio, true); // este cambio nunca lleva aviso (p. ej. volver a PROGRAMADA)
                continue;
            }
            avisar(cambio, tipo, null, destinatariosPara(tipo, clase));
            if (cambio.isNotificado()) {
                entregados++;
            }
        }
        return entregados;
    }

    /** Formato de la Fase 0: CLASE-{claseId}-CAMBIO-{cambioId}. */
    public String eventoId(CambioEstado cambio) {
        return "CLASE-" + cambio.getClaseId() + "-CAMBIO-" + cambio.getId();
    }

    /** Mensaje en español: actividad, fecha, motivo y nueva fecha si la hay. */
    public String construirMensaje(TipoEvento tipo, Clase clase, CambioEstado cambio) {
        String actividad = nombreActividad(clase);
        LocalDateTime fecha = cambio.getInicioAnterior() != null ? cambio.getInicioAnterior() : clase.getInicio();
        String base = "Tu clase de " + actividad + " del " + fecha.format(FORMATO_FECHA);
        String motivo = (cambio.getMotivo() == null || cambio.getMotivo().isBlank()) ? "" : ": " + cambio.getMotivo();
        String texto = switch (tipo) {
            case REPROGRAMACION -> base + " se reprogramó"
                    + (cambio.getInicioNuevo() != null ? " al " + cambio.getInicioNuevo().format(FORMATO_FECHA) : "")
                    + motivo;
            case SUSPENSION -> base + " se suspendió" + motivo;
            case SIN_FECHA -> base + " se suspendió y todavía no tiene nueva fecha" + motivo;
            case CANCELACION -> base + " fue cancelada" + motivo;
            case ALERTA_RIESGO -> base + " está en riesgo por el clima" + motivo
                    + ". Decidí si la mantenés o la suspendés";
        };
        return texto.endsWith(".") ? texto : texto + ".";
    }

    private String nombreActividad(Clase clase) {
        try {
            return actividades.obtenerActividad(clase.getTipoActividadId()).nombre();
        } catch (RuntimeException e) {
            return "actividad"; // no se frena el aviso porque no se pudo leer el nombre
        }
    }

    /** A qué tipo de aviso corresponde un cambio, según el estado al que pasó la clase. */
    private TipoEvento tipoPara(CambioEstado cambio) {
        if (cambio.getEstadoAnterior() == null) {
            return null; // creación de la clase
        }
        return switch (cambio.getEstadoNuevo()) {
            case SUSPENDIDA -> TipoEvento.SUSPENSION;
            case REPROGRAMADA -> TipoEvento.REPROGRAMACION;
            case SUSPENDIDA_SIN_FECHA -> TipoEvento.SIN_FECHA;
            case CANCELADA -> TipoEvento.CANCELACION;
            case EN_RIESGO -> TipoEvento.ALERTA_RIESGO;
            case PROGRAMADA -> null;
        };
    }

    /**
     * Destinatarios al reenviar. La alerta de riesgo va al profesor. Los demás avisos, a los inscriptos activos;
     * en una cancelación las inscripciones ya quedaron CANCELADA, así que se toman esas.
     */
    private List<String> destinatariosPara(TipoEvento tipo, Clase clase) {
        if (tipo == TipoEvento.ALERTA_RIESGO) {
            return List.of(clase.getProfesorId());
        }
        EstadoInscripcion estado = (tipo == TipoEvento.CANCELACION)
                ? EstadoInscripcion.CANCELADA : EstadoInscripcion.ACTIVA;
        return inscripciones.findByClaseIdAndEstado(clase.getId(), estado).stream()
                .map(Inscripcion::getAlumnoId)
                .toList();
    }

    private void marcar(CambioEstado cambio, boolean notificado) {
        cambio.setNotificado(notificado);
        cambios.save(cambio);
    }
}
