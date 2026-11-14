package ar.edu.utn.frc.climafit.notificaciones.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Un cambio de una clase que ms-clases pidió avisar. El id NO lo genera esta base: lo manda
 * ms-clases (por ejemplo "clase-7-cambio-15"), y si el mismo pedido llega dos veces (porque
 * ms-clases reintentó, RN16) se reconoce por ese id y no se duplica nada (RN15).
 */
@Entity
@Table(name = "evento_notificacion")
public class EventoNotificacion {

    @Id
    @Column(name = "evento_id")
    private String eventoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEvento tipo;

    @Column(name = "clase_id", nullable = false)
    private Long claseId;

    /** Dueño de la clase: con esto se controla quién puede ver las notificaciones (RN05). */
    @Column(name = "profesor_id", nullable = false)
    private String profesorId;

    @Column(nullable = false)
    private String mensaje;

    @Column(name = "fecha_recepcion", nullable = false)
    private LocalDateTime fechaRecepcion;

    protected EventoNotificacion() {
        // requerido por JPA
    }

    public EventoNotificacion(String eventoId, TipoEvento tipo, Long claseId, String profesorId, String mensaje) {
        this.eventoId = eventoId;
        this.tipo = tipo;
        this.claseId = claseId;
        this.profesorId = profesorId;
        this.mensaje = mensaje;
        this.fechaRecepcion = LocalDateTime.now();
    }

    public String getEventoId() { return eventoId; }
    public TipoEvento getTipo() { return tipo; }
    public Long getClaseId() { return claseId; }
    public String getProfesorId() { return profesorId; }
    public String getMensaje() { return mensaje; }
    public LocalDateTime getFechaRecepcion() { return fechaRecepcion; }
}
