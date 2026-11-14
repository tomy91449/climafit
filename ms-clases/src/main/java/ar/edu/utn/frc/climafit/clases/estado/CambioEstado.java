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

/** PROVISORIO (Persona 3, tarea 3.1): fila de cambio_estado. */
@Entity
@Table(name = "cambio_estado")
public class CambioEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clase_id")
    private Long claseId;

    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior")
    private EstadoClase estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo")
    private EstadoClase estadoNuevo;

    private String motivo;
    private String responsable;

    @Column(name = "inicio_anterior")
    private LocalDateTime inicioAnterior;

    @Column(name = "inicio_nuevo")
    private LocalDateTime inicioNuevo;

    private boolean notificado;

    protected CambioEstado() {
    }

    public CambioEstado(Long claseId, LocalDateTime fecha, EstadoClase estadoAnterior,
                        EstadoClase estadoNuevo, String motivo, String responsable) {
        this.claseId = claseId;
        this.fecha = fecha;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.motivo = motivo;
        this.responsable = responsable;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClaseId() { return claseId; }
    public EstadoClase getEstadoAnterior() { return estadoAnterior; }
    public EstadoClase getEstadoNuevo() { return estadoNuevo; }
    public String getMotivo() { return motivo; }
    public String getResponsable() { return responsable; }
    public LocalDateTime getInicioAnterior() { return inicioAnterior; }
    public void setInicioAnterior(LocalDateTime v) { this.inicioAnterior = v; }
    public LocalDateTime getInicioNuevo() { return inicioNuevo; }
    public void setInicioNuevo(LocalDateTime v) { this.inicioNuevo = v; }
    public boolean isNotificado() { return notificado; }
    public void setNotificado(boolean notificado) { this.notificado = notificado; }
}
