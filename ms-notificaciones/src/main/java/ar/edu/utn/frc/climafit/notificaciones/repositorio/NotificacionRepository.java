package ar.edu.utn.frc.climafit.notificaciones.repositorio;

import ar.edu.utn.frc.climafit.notificaciones.dominio.EstadoNotificacion;
import ar.edu.utn.frc.climafit.notificaciones.dominio.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    /** Destinatarios que YA tienen notificación para el evento (para ignorar los repetidos, RN15). */
    @Query("select n.usuarioId from Notificacion n where n.evento.eventoId = :eventoId")
    Set<String> findUsuariosNotificados(String eventoId);

    /** Todas las notificaciones de una clase, en orden cronológico. "join fetch" evita una consulta por evento. */
    @Query("""
            select n from Notificacion n join fetch n.evento e
            where e.claseId = :claseId
            order by e.fechaRecepcion, n.id""")
    List<Notificacion> findByClase(Long claseId);

    /** Trae también el evento (el mensaje a enviar), porque el envío corre fuera de una transacción. */
    @Query("select n from Notificacion n join fetch n.evento where n.id = :id")
    Optional<Notificacion> findConEvento(Long id);

    /** PENDIENTE que quedaron colgadas (por ejemplo, el servicio se reinició a mitad del envío). */
    @Query("""
            select n.id from Notificacion n
            where n.estado = :estado and n.evento.fechaRecepcion < :antesDe
            order by n.id""")
    List<Long> findIdsPorEstadoRecibidasAntesDe(EstadoNotificacion estado, LocalDateTime antesDe);
}
