package ar.edu.utn.frc.climafit.clases.clase;

import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * PROVISORIO (Persona 3). La firma de existeSuperposicionProfesor es la de la Fase 0, punto 9.
 * La Persona 4 solo la usa (RN04 en la búsqueda de nuevo día).
 */
public interface ClaseRepository extends JpaRepository<Clase, Long> {

    @Query(value = """
            SELECT EXISTS (
              SELECT 1 FROM clase c
              WHERE c.profesor_id = :profesorId
                AND c.estado <> 'CANCELADA'
                AND (CAST(:excluirClaseId AS BIGINT) IS NULL OR c.id <> :excluirClaseId)
                AND c.inicio < :fin
                AND c.inicio + c.duracion_min * INTERVAL '1 minute' > :inicio)
            """, nativeQuery = true)
    boolean existeSuperposicionProfesor(@Param("profesorId") String profesorId,
                                        @Param("inicio") LocalDateTime inicio,
                                        @Param("fin") LocalDateTime fin,
                                        @Param("excluirClaseId") Long excluirClaseId);
}
