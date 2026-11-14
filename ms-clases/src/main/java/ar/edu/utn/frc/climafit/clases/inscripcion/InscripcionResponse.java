package ar.edu.utn.frc.climafit.clases.inscripcion;

import java.time.LocalDateTime;

/** Campos de la Fase 0 (punto 8): id, claseId, alumnoId, estado, fechaAlta. */
public record InscripcionResponse(Long id, Long claseId, String alumnoId, EstadoInscripcion estado,
                                  LocalDateTime fechaAlta) {

    public static InscripcionResponse de(Inscripcion i) {
        return new InscripcionResponse(i.getId(), i.getClaseId(), i.getAlumnoId(), i.getEstado(), i.getFechaAlta());
    }
}
