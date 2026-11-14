package ar.edu.utn.frc.climafit.clases.clase;

import ar.edu.utn.frc.climafit.clases.estado.EstadoClase;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clase")
public class Clase {

    /**
     * Tope de duración de una clase (24 h). Permite buscar superposiciones mirando solo las clases que
     * empezaron en las últimas 24 h, sin hacer aritmética de fechas dentro de la consulta SQL.
     */
    public static final int DURACION_MAX_MIN = 1440;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Referencia lógica a ms-actividades (sin clave foránea: otra base). */
    @Column(name = "tipo_actividad_id", nullable = false)
    private Long tipoActividadId;

    /** Referencia lógica a ms-actividades (sin clave foránea: otra base). */
    @Column(name = "espacio_id", nullable = false)
    private Long espacioId;

    /** Copiado del espacio al crear la clase. */
    @Column(name = "al_aire_libre", nullable = false)
    private boolean alAireLibre;

    /** sub del profesor en Keycloak. */
    @Column(name = "profesor_id", nullable = false, length = 64)
    private String profesorId;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(name = "duracion_min", nullable = false)
    private int duracionMin;

    @Column(nullable = false)
    private int cupo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoClase estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_previo", length = 30)
    private EstadoClase estadoPrevio;

    /** Bloqueo optimista: si dos personas modifican la clase a la vez, la segunda falla. */
    @Version
    private Long version;

    @OneToMany(mappedBy = "clase", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("orden ASC")
    private List<RutinaItem> rutina = new ArrayList<>();

    public LocalDateTime getFin() {
        return inicio.plusMinutes(duracionMin);
    }

    public void agregarRutina(RutinaItem item) {
        item.setClase(this);
        rutina.add(item);
    }

    public void reemplazarRutina(List<RutinaItem> nuevos) {
        rutina.clear();
        nuevos.forEach(this::agregarRutina);
    }

    public Long getId() { return id; }
    public Long getTipoActividadId() { return tipoActividadId; }
    public void setTipoActividadId(Long tipoActividadId) { this.tipoActividadId = tipoActividadId; }
    public Long getEspacioId() { return espacioId; }
    public void setEspacioId(Long espacioId) { this.espacioId = espacioId; }
    public boolean isAlAireLibre() { return alAireLibre; }
    public void setAlAireLibre(boolean alAireLibre) { this.alAireLibre = alAireLibre; }
    public String getProfesorId() { return profesorId; }
    public void setProfesorId(String profesorId) { this.profesorId = profesorId; }
    public LocalDateTime getInicio() { return inicio; }
    public void setInicio(LocalDateTime inicio) { this.inicio = inicio; }
    public int getDuracionMin() { return duracionMin; }
    public void setDuracionMin(int duracionMin) { this.duracionMin = duracionMin; }
    public int getCupo() { return cupo; }
    public void setCupo(int cupo) { this.cupo = cupo; }
    public EstadoClase getEstado() { return estado; }
    public void setEstado(EstadoClase estado) { this.estado = estado; }
    public EstadoClase getEstadoPrevio() { return estadoPrevio; }
    public void setEstadoPrevio(EstadoClase estadoPrevio) { this.estadoPrevio = estadoPrevio; }
    public Long getVersion() { return version; }
    public List<RutinaItem> getRutina() { return rutina; }
}
