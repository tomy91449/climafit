package ar.edu.utn.frc.climafit.clases.estado;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Una fila del historial de la clase: quién cambió qué y cuándo. */
@Entity
@Table(name = "cambio_estado")
public class CambioEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clase_id", nullable = false)
    private Long claseId;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 30)
    private EstadoClase estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 30)
    private EstadoClase estadoNuevo;

    @Column(length = 500)
    private String motivo;

    /** sub del usuario o SISTEMA. */
    @Column(nullable = false, length = 64)
    private String responsable;

    @Column(name = "inicio_anterior")
    private LocalDateTime inicioAnterior;

    @Column(name = "inicio_nuevo")
    private LocalDateTime inicioNuevo;

    /** true cuando ms-notificaciones ya recibió el aviso de este cambio (RN16). */
    @Column(nullable = false)
    private boolean notificado;

    public Long getId() { return id; }
    public Long getClaseId() { return claseId; }
    public void setClaseId(Long claseId) { this.claseId = claseId; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public EstadoClase getEstadoAnterior() { return estadoAnterior; }
    public void setEstadoAnterior(EstadoClase estadoAnterior) { this.estadoAnterior = estadoAnterior; }
    public EstadoClase getEstadoNuevo() { return estadoNuevo; }
    public void setEstadoNuevo(EstadoClase estadoNuevo) { this.estadoNuevo = estadoNuevo; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getResponsable() { return responsable; }
    public void setResponsable(String responsable) { this.responsable = responsable; }
    public LocalDateTime getInicioAnterior() { return inicioAnterior; }
    public void setInicioAnterior(LocalDateTime inicioAnterior) { this.inicioAnterior = inicioAnterior; }
    public LocalDateTime getInicioNuevo() { return inicioNuevo; }
    public void setInicioNuevo(LocalDateTime inicioNuevo) { this.inicioNuevo = inicioNuevo; }
    public boolean isNotificado() { return notificado; }
    public void setNotificado(boolean notificado) { this.notificado = notificado; }
}
