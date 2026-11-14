package ar.edu.utn.frc.climafit.clases.inscripcion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {

    /** Firma de la Fase 0 (punto 9): la usa la Persona 4. */
    List<Inscripcion> findByClaseIdAndEstado(Long claseId, EstadoInscripcion estado);

    Optional<Inscripcion> findByClaseIdAndAlumnoId(Long claseId, String alumnoId);

    long countByClaseIdAndEstado(Long claseId, EstadoInscripcion estado);

    List<Inscripcion> findByAlumnoIdOrderByFechaAltaDesc(String alumnoId);

    /** Devuelve filas [claseId, cantidad] para calcular lugaresDisponibles de varias clases en una sola consulta. */
    @Query("select i.claseId, count(i) from Inscripcion i where i.claseId in :ids and i.estado = :estado "
            + "group by i.claseId")
    List<Object[]> contarPorClase(@Param("ids") Collection<Long> ids, @Param("estado") EstadoInscripcion estado);
}
