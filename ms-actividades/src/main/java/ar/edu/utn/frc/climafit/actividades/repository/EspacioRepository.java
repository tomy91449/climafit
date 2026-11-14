package ar.edu.utn.frc.climafit.actividades.repository;

import ar.edu.utn.frc.climafit.actividades.entity.Espacio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EspacioRepository extends JpaRepository<Espacio, Long> {

    List<Espacio> findByActivoTrueOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
