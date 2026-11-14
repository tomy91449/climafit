package ar.edu.utn.frc.climafit.clases.estado;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CambioEstadoRepository extends JpaRepository<CambioEstado, Long> {

    List<CambioEstado> findByClaseIdOrderByFechaAscIdAsc(Long claseId);

    /** Cambios cuyo aviso todavía no llegó a ms-notificaciones (los reenvía AvisoService, RN16). */
    List<CambioEstado> findByNotificadoFalseOrderByIdAsc();
}
