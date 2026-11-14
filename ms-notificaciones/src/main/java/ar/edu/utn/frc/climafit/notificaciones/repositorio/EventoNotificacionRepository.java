package ar.edu.utn.frc.climafit.notificaciones.repositorio;

import ar.edu.utn.frc.climafit.notificaciones.dominio.EventoNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventoNotificacionRepository extends JpaRepository<EventoNotificacion, String> {

    List<EventoNotificacion> findByClaseId(Long claseId);
}
