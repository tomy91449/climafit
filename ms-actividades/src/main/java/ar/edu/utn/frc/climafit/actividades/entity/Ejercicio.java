package ar.edu.utn.frc.climafit.actividades.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Ejercicio del catálogo (F04). Si {@code propietarioId} es NULL es un ejercicio base, cargado por el
 * administrador; si no, es un ejercicio personalizado del profesor con ese {@code sub} (RN08).
 * Las actividades con las que es compatible se usan para validar las rutinas de las clases (RN07).
 */
@Entity
@Table(name = "ejercicio")
public class Ejercicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 1000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Nivel nivel;

    @Column(name = "propietario_id", length = 64)
    private String propietarioId;

    @Column(nullable = false)
    private Boolean activo = Boolean.TRUE;

    @ManyToMany
    @JoinTable(name = "ejercicio_actividad",
            joinColumns = @JoinColumn(name = "ejercicio_id"),
            inverseJoinColumns = @JoinColumn(name = "tipo_actividad_id"))
    private Set<TipoActividad> actividades = new HashSet<>();

    protected Ejercicio() {
    }

    /**
     * @param propietarioId {@code null} para un ejercicio base; el {@code sub} del profesor para uno personalizado.
     */
    public Ejercicio(String nombre, String descripcion, Nivel nivel, String propietarioId,
                     Collection<TipoActividad> actividades) {
        this.propietarioId = propietarioId;
        this.activo = Boolean.TRUE;
        actualizar(nombre, descripcion, nivel, actividades);
    }

    public void actualizar(String nombre, String descripcion, Nivel nivel, Collection<TipoActividad> actividades) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.nivel = nivel;
        this.actividades.clear();
        this.actividades.addAll(actividades);
    }

    /** Baja lógica: las rutinas que ya lo usan no se ven afectadas. */
    public void darDeBaja() {
        this.activo = Boolean.FALSE;
    }

    public boolean esBase() {
        return propietarioId == null;
    }

    public boolean esDe(String usuarioId) {
        return propietarioId != null && propietarioId.equals(usuarioId);
    }

    public boolean estaActivo() {
        return Boolean.TRUE.equals(activo);
    }

    public boolean esCompatibleCon(Long actividadId) {
        return actividades.stream().anyMatch(actividad -> Objects.equals(actividad.getId(), actividadId));
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Nivel getNivel() {
        return nivel;
    }

    public String getPropietarioId() {
        return propietarioId;
    }

    public Boolean getActivo() {
        return activo;
    }

    public Set<TipoActividad> getActividades() {
        return actividades;
    }
}
