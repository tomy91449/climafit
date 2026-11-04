package ar.edu.utn.frc.climafit.actividades.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Tipo de actividad con sus criterios climáticos. Estos tres límites son los que ms-clima
 * compara contra el pronóstico (RN10): por eso "no es lo mismo yoga que running".
 * <p>
 * La entidad nunca sale del microservicio: hacia afuera se exponen DTOs. Así se puede cambiar
 * la tabla sin romper el contrato de la API.
 */
@Entity
@Table(name = "tipo_actividad")
public class TipoActividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    private String descripcion;

    /** Probabilidad de lluvia máxima tolerada, en % (0-100). */
    @Column(name = "prob_lluvia_max", nullable = false)
    private int probLluviaMax;

    @Column(name = "viento_max_kmh", nullable = false)
    private int vientoMaxKmh;

    @Column(name = "temp_min_c", nullable = false)
    private int tempMinC;

    /** Baja lógica: una actividad dada de baja no se usa en clases nuevas, pero las viejas la siguen referenciando. */
    @Column(nullable = false)
    private boolean activo = true;

    protected TipoActividad() {
        // requerido por JPA
    }

    public TipoActividad(String nombre, String descripcion, int probLluviaMax, int vientoMaxKmh, int tempMinC) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.probLluviaMax = probLluviaMax;
        this.vientoMaxKmh = vientoMaxKmh;
        this.tempMinC = tempMinC;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public int getProbLluviaMax() { return probLluviaMax; }
    public int getVientoMaxKmh() { return vientoMaxKmh; }
    public int getTempMinC() { return tempMinC; }
    public boolean isActivo() { return activo; }
}
