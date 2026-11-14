package ar.edu.utn.frc.climafit.actividades.repository;

import ar.edu.utn.frc.climafit.actividades.entity.TipoActividad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TipoActividadRepository extends JpaRepository<TipoActividad, Long> {

    List<TipoActividad> findByActivoTrueOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
