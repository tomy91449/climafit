package ar.edu.utn.frc.climafit.clases.evaluacion;

import ar.edu.utn.frc.climafit.clases.clase.Clase;
import ar.edu.utn.frc.climafit.clases.client.actividades.ActividadDto;
import ar.edu.utn.frc.climafit.clases.client.actividades.EspacioDto;
import ar.edu.utn.frc.climafit.clases.estado.CambioEstado;
import ar.edu.utn.frc.climafit.clases.estado.EstadoClase;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Datos de prueba compartidos (hora fija: sábado 14/11/2026 12:00, hora de Córdoba). */
public final class Datos {

    public static final ZoneId ZONA = ZoneId.of("America/Argentina/Cordoba");
    public static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 14, 12, 0);
    public static final Clock RELOJ = Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA);

    /** Yoga con los criterios provisorios del cambio de dominio. */
    public static final ActividadDto YOGA = new ActividadDto(1L, "Yoga", 15, 22, 14, 0.0, true);
    public static final EspacioDto SARMIENTO = new EspacioDto(1L, "Parque Sarmiento", -31.4290, -64.1800, true);

    private Datos() {
    }

    /** Clase de Yoga al aire libre del profe1 que empieza dentro de {@code horas} horas. */
    public static Clase clase(long id, int horas) {
        Clase c = new Clase(1L, 1L, true, "profe1", AHORA.plusHours(horas), 60, 10);
        c.setId(id);
        return c;
    }

    public static CambioEstado cambio(long id, long claseId, EstadoClase de, EstadoClase a) {
        CambioEstado c = new CambioEstado(claseId, AHORA, de, a, "motivo", "SISTEMA");
        c.setId(id);
        return c;
    }
}
