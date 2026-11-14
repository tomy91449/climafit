package ar.edu.utn.frc.climafit.actividades.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Espacio donde se dictan las clases (F03). Las coordenadas se usan para consultar el pronóstico;
 * {@code alAireLibre} indica si la clase se evalúa según el clima (RN09).
 */
@Entity
@Table(name = "espacio")
public class Espacio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(length = 255)
    private String direccion;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitud;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitud;

    @Column(name = "al_aire_libre", nullable = false)
    private Boolean alAireLibre;

    @Column(nullable = false)
    private Boolean activo = Boolean.TRUE;

    protected Espacio() {
    }

    public Espacio(String nombre, String direccion, BigDecimal latitud, BigDecimal longitud, Boolean alAireLibre) {
        actualizar(nombre, direccion, latitud, longitud, alAireLibre);
        this.activo = Boolean.TRUE;
    }

    public void actualizar(String nombre, String direccion, BigDecimal latitud, BigDecimal longitud,
                           Boolean alAireLibre) {
        this.nombre = nombre;
        this.direccion = direccion;
        this.latitud = latitud;
        this.longitud = longitud;
        this.alAireLibre = alAireLibre;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public BigDecimal getLatitud() {
        return latitud;
    }

    public BigDecimal getLongitud() {
        return longitud;
    }

    public Boolean getAlAireLibre() {
        return alAireLibre;
    }

    public Boolean getActivo() {
        return activo;
    }
}
