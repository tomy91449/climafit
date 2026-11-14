package ar.edu.utn.frc.climafit.clases.evaluacion;

import ar.edu.utn.frc.climafit.clases.clase.Clase;
import ar.edu.utn.frc.climafit.clases.estado.EstadoClase;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Consulta de P1 (RN09). Es un repositorio propio de la Persona 4 sobre la entidad Clase para
 * no tocar el ClaseRepository de la Persona 3 (regla: cada uno toca solo sus paquetes).
 */
public interface ClasesAEvaluarRepository extends JpaRepository<Clase, Long> {

    @Query("""
            SELECT c FROM Clase c
            WHERE c.alAireLibre = true
              AND c.estado IN :estados
              AND c.inicio >= :desde
              AND c.inicio <= :hasta
            ORDER BY c.inicio
            """)
    List<Clase> buscarAEvaluar(@Param("estados") Collection<EstadoClase> estados,
                               @Param("desde") LocalDateTime desde,
                               @Param("hasta") LocalDateTime hasta);
}
