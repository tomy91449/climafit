package ar.edu.utn.frc.climafit.clases.clase;

import ar.edu.utn.frc.climafit.clases.estado.EstadoClase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

/**
 * PROVISORIO (Persona 3, tarea 3.1): entidad clase con las columnas del DER.
 * Reemplazar por la real. La Persona 4 usa solo estos accesores.
 */
@Entity
@Table(name = "clase")
public class Clase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_actividad_id")
    private Long tipoActividadId;

    @Column(name = "espacio_id")
    private Long espacioId;

    @Column(name = "al_aire_libre")
    private boolean alAireLibre;

    @Column(name = "profesor_id")
    private String profesorId;

    private LocalDateTime inicio;

    @Column(name = "duracion_min")
    private int duracionMin;

    private int cupo;

    @Enumerated(EnumType.STRING)
    private EstadoClase estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_previo")
    private EstadoClase estadoPrevio;

    @Version
    private Long version;

    protected Clase() {
    }

    public Clase(Long tipoActividadId, Long espacioId, boolean alAireLibre, String profesorId,
                 LocalDateTime inicio, int duracionMin, int cupo) {
        this.tipoActividadId = tipoActividadId;
        this.espacioId = espacioId;
        this.alAireLibre = alAireLibre;
        this.profesorId = profesorId;
        this.inicio = inicio;
        this.duracionMin = duracionMin;
        this.cupo = cupo;
        this.estado = EstadoClase.PROGRAMADA;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTipoActividadId() { return tipoActividadId; }
    public Long getEspacioId() { return espacioId; }
    public boolean isAlAireLibre() { return alAireLibre; }
    public String getProfesorId() { return profesorId; }
    public LocalDateTime getInicio() { return inicio; }
    public void setInicio(LocalDateTime inicio) { this.inicio = inicio; }
    public int getDuracionMin() { return duracionMin; }
    public int getCupo() { return cupo; }
    public EstadoClase getEstado() { return estado; }
    public void setEstado(EstadoClase estado) { this.estado = estado; }
    public EstadoClase getEstadoPrevio() { return estadoPrevio; }
    public void setEstadoPrevio(EstadoClase estadoPrevio) { this.estadoPrevio = estadoPrevio; }

    /** Hora de fin = inicio + duración. */
    public LocalDateTime getFin() {
        return inicio.plusMinutes(duracionMin);
    }
}
