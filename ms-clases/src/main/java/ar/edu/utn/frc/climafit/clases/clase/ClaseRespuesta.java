package ar.edu.utn.frc.climafit.clases.clase;

import ar.edu.utn.frc.climafit.clases.estado.EstadoClase;
import java.time.LocalDateTime;

/** PROVISORIO (Persona 3): cuerpo de respuesta de una clase (Fase 0, punto 8), sin la rutina. */
public record ClaseRespuesta(Long id, Long tipoActividadId, Long espacioId, LocalDateTime inicio,
                             int duracionMinutos, int cupo, boolean alAireLibre, String profesorId,
                             EstadoClase estado, int lugaresDisponibles) {

    public static ClaseRespuesta de(Clase c, int inscriptosActivos) {
        return new ClaseRespuesta(c.getId(), c.getTipoActividadId(), c.getEspacioId(), c.getInicio(),
                c.getDuracionMin(), c.getCupo(), c.isAlAireLibre(), c.getProfesorId(), c.getEstado(),
                c.getCupo() - inscriptosActivos);
    }
}
