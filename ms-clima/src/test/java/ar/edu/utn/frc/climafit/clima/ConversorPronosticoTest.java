package ar.edu.utn.frc.climafit.clima;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.frc.climafit.clima.modelo.HoraPronostico;
import ar.edu.utn.frc.climafit.clima.openmeteo.ConversorPronostico;
import ar.edu.utn.frc.climafit.clima.openmeteo.RespuestaOpenMeteo;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConversorPronosticoTest {

    @Test
    void conviertePronosticoHorarioDeOpenMeteo() {
        RespuestaOpenMeteo r = new RespuestaOpenMeteo(new RespuestaOpenMeteo.Hourly(
                List.of("2026-11-20T18:00", "2026-11-20T19:00"),
                List.of(21.6, 12.4),             // temperatura
                List.of(80, 10),                 // probabilidad de lluvia
                List.of(1.25, 0.0),              // mm
                List.of(24.6, 9.2),              // viento km/h
                List.of(61, 0)));                // código de tiempo

        List<HoraPronostico> horas = ConversorPronostico.aHoras(r);

        assertEquals(2, horas.size());
        HoraPronostico h = horas.get(0);
        assertEquals(LocalDateTime.of(2026, 11, 20, 18, 0), h.fechaHora());
        assertEquals(80, h.probLluvia());
        assertEquals(25, h.vientoKmh());          // 24,6 se redondea
        assertEquals(22, h.temperaturaC());       // 21,6 se redondea
        assertEquals(1.25, h.precipitacionMm(), 0.0001);
        assertFalse(h.tormenta());
    }

    @Test
    void lasHorasConCodigoDeTormentaMarcanTormenta() {
        RespuestaOpenMeteo r = new RespuestaOpenMeteo(new RespuestaOpenMeteo.Hourly(
                List.of("2026-11-20T18:00", "2026-11-20T19:00", "2026-11-20T20:00", "2026-11-20T21:00"),
                List.of(20.0, 20.0, 20.0, 20.0), List.of(0, 0, 0, 0), List.of(0.0, 0.0, 0.0, 0.0),
                List.of(5.0, 5.0, 5.0, 5.0), List.of(95, 96, 99, 63)));
        List<HoraPronostico> horas = ConversorPronostico.aHoras(r);
        assertTrue(horas.get(0).tormenta());
        assertTrue(horas.get(1).tormenta());
        assertTrue(horas.get(2).tormenta());
        assertFalse(horas.get(3).tormenta());     // 63 es lluvia moderada, no tormenta
    }

    @Test
    void unValorNuloDeOpenMeteoCuentaComoCero() {
        RespuestaOpenMeteo r = new RespuestaOpenMeteo(new RespuestaOpenMeteo.Hourly(
                List.of("2026-11-20T18:00"), List.of(20.0), Arrays.asList((Integer) null),
                Arrays.asList((Double) null), List.of(5.0), List.of(0)));
        HoraPronostico h = ConversorPronostico.aHoras(r).get(0);
        assertEquals(0, h.probLluvia());
        assertEquals(0.0, h.precipitacionMm(), 0.0001);
    }

    @Test
    void unaRespuestaSinDatosHorariosDaListaVacia() {
        assertTrue(ConversorPronostico.aHoras(new RespuestaOpenMeteo(null)).isEmpty());
    }
}
