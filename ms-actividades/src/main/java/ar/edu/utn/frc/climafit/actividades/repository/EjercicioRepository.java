package ar.edu.utn.frc.climafit.actividades.repository;

import ar.edu.utn.frc.climafit.actividades.entity.Ejercicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EjercicioRepository extends JpaRepository<Ejercicio, Long> {

    /**
     * Ejercicios activos que puede ver un usuario (RN08): los base y los personalizados suyos.
     * Si {@code actividadId} no es null, solo los compatibles con esa actividad.
     */
    @Query("""
            select distinct e from Ejercicio e left join e.actividades a
            where e.activo = true
              and (e.propietarioId is null or e.propietarioId = :usuarioId)
              and (:actividadId is null or a.id = :actividadId)
            order by e.nombre
            """)
    List<Ejercicio> buscarVisibles(@Param("usuarioId") String usuarioId, @Param("actividadId") Long actividadId);
}
