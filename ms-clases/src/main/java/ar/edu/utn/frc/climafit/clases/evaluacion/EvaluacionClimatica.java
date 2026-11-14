package ar.edu.utn.frc.climafit.clases.evaluacion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Entidad JPA que mapea una fila de evaluacion_climatica. Conserva los resultados para consultar
 * el historial; en una evaluación FALLIDA los valores del pronóstico quedan nulos.
 */
@Entity
@Table(name = "evaluacion_climatica")
public class EvaluacionClimatica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clase_id")
    private Long claseId;

    @Column(name = "fecha_evaluacion")
    private LocalDateTime fechaEvaluacion;

    @Column(name = "inicio_evaluado")
    private LocalDateTime inicioEvaluado;

    @Enumerated(EnumType.STRING)
    private ResultadoEvaluacion resultado;

    @Column(length = 500)
    private String motivo;

    @Column(name = "prob_lluvia")
    private Integer probLluvia;

    @Column(name = "viento_kmh")
    private Integer vientoKmh;

    @Column(name = "temperatura_c")
    private Integer temperaturaC;

    private Boolean tormenta;

    @Column(name = "precipitacion_mm")
    private Double precipitacionMm;

    protected EvaluacionClimatica() {
    }

    /** Construye una evaluación persistible y limita el motivo al tamaño definido por el esquema. */
    public EvaluacionClimatica(Long claseId, LocalDateTime fechaEvaluacion, LocalDateTime inicioEvaluado,
                               ResultadoEvaluacion resultado, String motivo, Integer probLluvia,
                               Integer vientoKmh, Integer temperaturaC, Boolean tormenta,
                               Double precipitacionMm) {
        this.claseId = claseId;
        this.fechaEvaluacion = fechaEvaluacion;
        this.inicioEvaluado = inicioEvaluado;
        this.resultado = resultado;
        this.motivo = motivo == null || motivo.length() <= 500 ? motivo : motivo.substring(0, 500);
        this.probLluvia = probLluvia;
        this.vientoKmh = vientoKmh;
        this.temperaturaC = temperaturaC;
        this.tormenta = tormenta;
        this.precipitacionMm = precipitacionMm;
    }

    public Long getId() { return id; }
    public Long getClaseId() { return claseId; }
    public LocalDateTime getFechaEvaluacion() { return fechaEvaluacion; }
    public LocalDateTime getInicioEvaluado() { return inicioEvaluado; }
    public ResultadoEvaluacion getResultado() { return resultado; }
    public String getMotivo() { return motivo; }
    public Integer getProbLluvia() { return probLluvia; }
    public Integer getVientoKmh() { return vientoKmh; }
    public Integer getTemperaturaC() { return temperaturaC; }
    public Boolean getTormenta() { return tormenta; }
    public Double getPrecipitacionMm() { return precipitacionMm; }
}
