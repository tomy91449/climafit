package ar.edu.utn.frc.climafit.clases.clase;

import ar.edu.utn.frc.climafit.clases.estado.EstadoClase;

import java.time.LocalDateTime;
import java.util.List;

/** Respuesta de clases: los mismos campos del pedido más alAireLibre, profesorId, estado y lugaresDisponibles. */
public record ClaseResponse(Long id, Long tipoActividadId, Long espacioId, LocalDateTime inicio,
                            int duracionMinutos, int cupo, List<RutinaItemDto> rutina, boolean alAireLibre,
                            String profesorId, EstadoClase estado, int lugaresDisponibles) {

    public static ClaseResponse de(Clase clase, int lugaresDisponibles) {
        List<RutinaItemDto> rutina = clase.getRutina().stream()
                .map(r -> new RutinaItemDto(r.getEjercicioId(), r.getOrden(), r.getIndicaciones()))
                .toList();
        return new ClaseResponse(clase.getId(), clase.getTipoActividadId(), clase.getEspacioId(), clase.getInicio(),
                clase.getDuracionMin(), clase.getCupo(), rutina, clase.isAlAireLibre(), clase.getProfesorId(),
                clase.getEstado(), lugaresDisponibles);
    }
}
