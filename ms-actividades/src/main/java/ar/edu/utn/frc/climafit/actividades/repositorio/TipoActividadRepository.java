package ar.edu.utn.frc.climafit.actividades.repositorio;

import ar.edu.utn.frc.climafit.actividades.dominio.TipoActividad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Spring Data genera la implementación a partir del nombre de cada método. */
public interface TipoActividadRepository extends JpaRepository<TipoActividad, Long> {

    List<TipoActividad> findByActivoTrueOrderByNombre();

    boolean existsByNombreIgnoreCase(String nombre);
}
