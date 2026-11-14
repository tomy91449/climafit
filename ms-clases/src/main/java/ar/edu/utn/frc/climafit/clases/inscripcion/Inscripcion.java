package ar.edu.utn.frc.climafit.clases.inscripcion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** PROVISORIO (Persona 3). La Persona 4 solo lee alumnoId y estado. */
@Entity
@Table(name = "inscripcion")
public class Inscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clase_id")
    private Long claseId;

    @Column(name = "alumno_id")
    private String alumnoId;

    @Enumerated(EnumType.STRING)
    private EstadoInscripcion estado;

    @Column(name = "fecha_alta")
    private LocalDateTime fechaAlta;

    protected Inscripcion() {
    }

    public Inscripcion(Long claseId, String alumnoId, EstadoInscripcion estado, LocalDateTime fechaAlta) {
        this.claseId = claseId;
        this.alumnoId = alumnoId;
        this.estado = estado;
        this.fechaAlta = fechaAlta;
    }

    public Long getId() { return id; }
    public Long getClaseId() { return claseId; }
    public String getAlumnoId() { return alumnoId; }
    public EstadoInscripcion getEstado() { return estado; }
}
