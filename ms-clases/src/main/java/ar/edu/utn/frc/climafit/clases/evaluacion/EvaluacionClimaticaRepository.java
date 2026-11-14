package ar.edu.utn.frc.climafit.clases.evaluacion;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso JPA a evaluacion_climatica. Spring Data implementa las consultas derivadas a partir
 * de sus nombres, mientras JpaRepository aporta el guardado y la búsqueda por identificador.
 */
public interface EvaluacionClimaticaRepository extends JpaRepository<EvaluacionClimatica, Long> {

    /** Lista el historial climático de una clase, primero con la evaluación más reciente. */
    List<EvaluacionClimatica> findByClaseIdOrderByFechaEvaluacionDesc(Long claseId);

    /** Última evaluación que sí pudo hacerse (sirve para el motivo de la decisión del profesor). */
    Optional<EvaluacionClimatica> findFirstByClaseIdAndResultadoNotOrderByFechaEvaluacionDesc(
            Long claseId, ResultadoEvaluacion excluido);
}
