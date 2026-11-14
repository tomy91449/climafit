package ar.edu.utn.frc.climafit.clases.estado;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** PROVISORIO (Persona 3). */
public interface CambioEstadoRepository extends JpaRepository<CambioEstado, Long> {

    List<CambioEstado> findByNotificadoFalse();
}
