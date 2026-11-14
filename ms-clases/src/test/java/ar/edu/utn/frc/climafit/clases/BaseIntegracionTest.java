package ar.edu.utn.frc.climafit.clases;

import ar.edu.utn.frc.climafit.clases.aviso.NotificacionesClientStub;
import ar.edu.utn.frc.climafit.clases.clase.Clase;
import ar.edu.utn.frc.climafit.clases.clase.ClaseRepository;
import ar.edu.utn.frc.climafit.clases.estado.CambioEstadoRepository;
import ar.edu.utn.frc.climafit.clases.estado.EstadoClase;
import ar.edu.utn.frc.climafit.clases.inscripcion.EstadoInscripcion;
import ar.edu.utn.frc.climafit.clases.inscripcion.Inscripcion;
import ar.edu.utn.frc.climafit.clases.inscripcion.InscripcionRepository;
import ar.edu.utn.frc.climafit.commons.error.NegocioException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Base de los tests: levanta Spring con H2 y los clientes falsos (perfil stub) y deja las tablas vacías. */
@SpringBootTest
@ActiveProfiles({"test", "stub"})
public abstract class BaseIntegracionTest {

    protected static final String PROFE_1 = "profe1-sub";
    protected static final String PROFE_2 = "profe2-sub";
    protected static final String ALUMNO_1 = "alumno1-sub";
    protected static final String ALUMNO_2 = "alumno2-sub";
    protected static final String ALUMNO_3 = "alumno3-sub";

    @Autowired protected ClaseRepository clases;
    @Autowired protected InscripcionRepository inscripciones;
    @Autowired protected CambioEstadoRepository cambios;
    @Autowired protected NotificacionesClientStub notificaciones;

    @BeforeEach
    void limpiarTablas() {
        cambios.deleteAll();
        inscripciones.deleteAll();
        clases.deleteAll();
        notificaciones.limpiar();
    }

    /** Mañana a la hora indicada (siempre en el futuro, así las clases no "ya empezaron"). */
    protected static LocalDateTime manana(int hora) {
        return LocalDate.now().plusDays(1).atTime(hora, 0);
    }

    /** Guarda una clase directo en la base (sin pasar por las reglas de POST /clases). */
    protected Clase claseEnBase(String profesor, LocalDateTime inicio, int duracionMin, int cupo, EstadoClase estado) {
        Clase clase = new Clase();
        clase.setTipoActividadId(1L);
        clase.setEspacioId(1L);
        clase.setAlAireLibre(true);
        clase.setProfesorId(profesor);
        clase.setInicio(inicio);
        clase.setDuracionMin(duracionMin);
        clase.setCupo(cupo);
        clase.setEstado(estado);
        return clases.save(clase);
    }

    protected Inscripcion inscripcionEnBase(Long claseId, String alumno) {
        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setClaseId(claseId);
        inscripcion.setAlumnoId(alumno);
        inscripcion.setEstado(EstadoInscripcion.ACTIVA);
        inscripcion.setFechaAlta(LocalDateTime.now().withNano(0));
        return inscripciones.save(inscripcion);
    }

    /** Verifica que la acción falle con el código y el status del contrato de errores. */
    protected static void assertError(ThrowingCallable accion, String codigo, HttpStatus status) {
        assertThatThrownBy(accion).isInstanceOfSatisfying(NegocioException.class, e -> {
            assertThat(e.getCodigo()).isEqualTo(codigo);
            assertThat(e.getStatus()).isEqualTo(status);
        });
    }
}
