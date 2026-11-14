package ar.edu.utn.frc.climafit.clases.evaluacion;

import ar.edu.utn.frc.climafit.clases.clase.Clase;
import ar.edu.utn.frc.climafit.clases.client.actividades.ActividadDto;
import ar.edu.utn.frc.climafit.clases.client.actividades.EspacioDto;
import ar.edu.utn.frc.climafit.clases.estado.CambioEstado;
import ar.edu.utn.frc.climafit.clases.estado.EstadoClase;
import ar.edu.utn.frc.climafit.clases.inscripcion.EstadoInscripcion;
import ar.edu.utn.frc.climafit.clases.inscripcion.Inscripcion;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.test.util.ReflectionTestUtils;

/** Datos de prueba compartidos (hora fija: sábado 14/11/2026 12:00, hora de Córdoba). */
public final class Datos {

    public static final ZoneId ZONA = ZoneId.of("America/Argentina/Cordoba");
    public static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 14, 12, 0);
    public static final Clock RELOJ = Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA);

    /** Yoga con los criterios provisorios del cambio de dominio. */
    public static final ActividadDto YOGA = new ActividadDto(1L, "Yoga", null, 15.0, 22.0, 14.0, 0.0, true);
    public static final EspacioDto SARMIENTO =
            new EspacioDto(1L, "Parque Sarmiento", null, -31.4290, -64.1800, true, true);

    private Datos() {
    }

    /** Clase de Yoga al aire libre del profe1 que empieza dentro de {@code horas} horas. */
    public static Clase clase(long id, int horas) {
        return clase(id, 1L, true, AHORA.plusHours(horas));
    }

    /** Clase PROGRAMADA del profe1 (60 minutos, cupo 10) con el id ya asignado, como si viniera de la base. */
    public static Clase clase(long id, long espacioId, boolean alAireLibre, LocalDateTime inicio) {
        Clase c = new Clase();
        c.setTipoActividadId(1L);
        c.setEspacioId(espacioId);
        c.setAlAireLibre(alAireLibre);
        c.setProfesorId("profe1");
        c.setInicio(inicio);
        c.setDuracionMin(60);
        c.setCupo(10);
        c.setEstado(EstadoClase.PROGRAMADA);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    public static CambioEstado cambio(long id, long claseId, EstadoClase de, EstadoClase a) {
        CambioEstado c = new CambioEstado();
        c.setClaseId(claseId);
        c.setFecha(AHORA);
        c.setEstadoAnterior(de);
        c.setEstadoNuevo(a);
        c.setMotivo("motivo");
        c.setResponsable("SISTEMA");
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    public static Inscripcion inscripcion(long claseId, String alumnoId, LocalDateTime fechaAlta) {
        Inscripcion i = new Inscripcion();
        i.setClaseId(claseId);
        i.setAlumnoId(alumnoId);
        i.setEstado(EstadoInscripcion.ACTIVA);
        i.setFechaAlta(fechaAlta);
        return i;
    }
}
