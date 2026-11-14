package ar.edu.utn.frc.climafit.clases.inscripcion;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** PROVISORIO (Persona 3). Firma de la Fase 0, punto 9. */
public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {

    List<Inscripcion> findByClaseIdAndEstado(Long claseId, EstadoInscripcion estado);
}
