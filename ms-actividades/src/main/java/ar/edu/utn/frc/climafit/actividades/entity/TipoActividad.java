package ar.edu.utn.frc.climafit.actividades.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Tipo de actividad (Yoga, Running...) con sus criterios climáticos (F02). ms-clima los usa para decidir
 * si una clase es apta (RN10):
 * <ul>
 *   <li>lluvia: la clase no se dicta si es probable que llueva ({@code probLluviaMax}, en %) y además
 *       llueve más de lo que la actividad tolera ({@code lluviaMaxMmHora}, en mm por hora);</li>
 *   <li>viento máximo y temperatura mínima.</li>
 * </ul>
 */
@Entity
@Table(name = "tipo_actividad")
public class TipoActividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    /** Probabilidad de lluvia (0 a 100 %) a partir de la cual se considera que va a llover. */
    @Column(name = "prob_lluvia_max", nullable = false)
    private Integer probLluviaMax;

    /** Milímetros de lluvia por hora que la actividad tolera (0 = no tolera ninguna lluvia). */
    @Column(name = "lluvia_max_mm_hora", nullable = false, precision = 4, scale = 1)
    private BigDecimal lluviaMaxMmHora;

    @Column(name = "viento_max_kmh", nullable = false)
    private Integer vientoMaxKmh;

    @Column(name = "temp_min_c", nullable = false)
    private Integer tempMinC;

    /** Baja lógica: una actividad inactiva no se usa en nuevas clases. */
    @Column(nullable = false)
    private Boolean activo = Boolean.TRUE;

    protected TipoActividad() {
    }

    public TipoActividad(String nombre, String descripcion, Integer probLluviaMax, BigDecimal lluviaMaxMmHora,
                         Integer vientoMaxKmh, Integer tempMinC) {
        actualizar(nombre, descripcion, probLluviaMax, lluviaMaxMmHora, vientoMaxKmh, tempMinC);
        this.activo = Boolean.TRUE;
    }

    public void actualizar(String nombre, String descripcion, Integer probLluviaMax, BigDecimal lluviaMaxMmHora,
                           Integer vientoMaxKmh, Integer tempMinC) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.probLluviaMax = probLluviaMax;
        this.lluviaMaxMmHora = lluviaMaxMmHora;
        this.vientoMaxKmh = vientoMaxKmh;
        this.tempMinC = tempMinC;
    }

    public void darDeBaja() {
        this.activo = Boolean.FALSE;
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

    public Integer getProbLluviaMax() {
        return probLluviaMax;
    }

    public BigDecimal getLluviaMaxMm() {
        return lluviaMaxMmHora;
    }

    public Integer getVientoMaxKmh() {
        return vientoMaxKmh;
    }

    public Integer getTempMinC() {
        return tempMinC;
    }

    public Boolean getActivo() {
        return activo;
    }

    public boolean estaActivo() {
        return Boolean.TRUE.equals(activo);
    }
}
