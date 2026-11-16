package ar.edu.utn.frc.climafit.notificaciones.repositorio;

import ar.edu.utn.frc.climafit.notificaciones.dominio.EventoNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoNotificacionRepository extends JpaRepository<EventoNotificacion, String> {
}
