package ar.edu.utn.frc.climafit.clases.clase;

import ar.edu.utn.frc.climafit.clases.estado.EstadoClase;
import ar.edu.utn.frc.climafit.clases.inscripcion.EstadoInscripcion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClaseRepository extends JpaRepository<Clase, Long>, JpaSpecificationExecutor<Clase> {

    /** SELECT ... FOR UPDATE: nadie más puede tocar esta clase hasta que termine la transacción (RN01). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Clase c where c.id = :id")
    Optional<Clase> findByIdForUpdate(@Param("id") Long id);

    @Query("select c from Clase c where c.profesorId = :profesorId and c.estado in :estados "
            + "and c.inicio < :fin and c.inicio > :desde")
    List<Clase> candidatasDelProfesor(@Param("profesorId") String profesorId,
                                      @Param("estados") Collection<EstadoClase> estados,
                                      @Param("fin") LocalDateTime fin,
                                      @Param("desde") LocalDateTime desde);

    @Query("select c from Clase c where c.id in (select i.claseId from Inscripcion i "
            + "where i.alumnoId = :alumnoId and i.estado = :estadoInscripcion) "
            + "and c.estado in :estados and c.inicio < :fin and c.inicio > :desde")
    List<Clase> candidatasDelAlumno(@Param("alumnoId") String alumnoId,
                                    @Param("estadoInscripcion") EstadoInscripcion estadoInscripcion,
                                    @Param("estados") Collection<EstadoClase> estados,
                                    @Param("fin") LocalDateTime fin,
                                    @Param("desde") LocalDateTime desde);

    /**
     * RN04. Firma de la Fase 0 (punto 9); la reutiliza la Persona 4 en la reprogramación automática.
     * Dos clases se superponen si una empieza antes de que termine la otra. Solo cuentan las clases vigentes
     * (PROGRAMADA, EN_RIESGO, REPROGRAMADA).
     */
    default boolean existeSuperposicionProfesor(String profesorId, LocalDateTime inicio, LocalDateTime fin,
                                                Long excluirClaseId) {
        return candidatasDelProfesor(profesorId, EstadoClase.VIGENTES, fin,
                inicio.minusMinutes(Clase.DURACION_MAX_MIN)).stream()
                .filter(c -> excluirClaseId == null || !excluirClaseId.equals(c.getId()))
                .anyMatch(c -> c.getFin().isAfter(inicio));
    }

    /** RN03: ¿el alumno ya tiene otra inscripción ACTIVA que se pise con este horario? */
    default boolean existeSuperposicionAlumno(String alumnoId, LocalDateTime inicio, LocalDateTime fin,
                                              Long excluirClaseId) {
        return candidatasDelAlumno(alumnoId, EstadoInscripcion.ACTIVA, EstadoClase.VIGENTES, fin,
                inicio.minusMinutes(Clase.DURACION_MAX_MIN)).stream()
                .filter(c -> excluirClaseId == null || !excluirClaseId.equals(c.getId()))
                .anyMatch(c -> c.getFin().isAfter(inicio));
    }
}
