package ar.edu.utn.frc.climafit.clima;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.frc.climafit.clima.fuente.FuenteSimulada;
import ar.edu.utn.frc.climafit.clima.modelo.Escenario;
import ar.edu.utn.frc.climafit.clima.modelo.HoraPronostico;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class FuenteSimuladaTest {

    private static final double LAT = -31.4290;
    private static final double LON = -64.1800;
    private static final LocalDateTime BASE = LocalDateTime.of(2026, 11, 20, 12, 0);

    private final FuenteSimulada fuente = new FuenteSimulada();

    private static Escenario lluvia(double lat, double lon, int desdeH, int hastaH, int prob) {
        return new Escenario(lat, lon, BASE.plusHours(desdeH), BASE.plusHours(hastaH), prob, 10, 18, false, 0.0);
    }

    @Test
    void sinEscenariosDevuelveUnTiempoAptoPorDefecto() {
        List<HoraPronostico> horas = fuente.obtener(LAT, LON, BASE, BASE.plusHours(3));
        assertEquals(3, horas.size());
        HoraPronostico h = horas.get(0);
        assertEquals(0, h.probLluvia());
        assertEquals(5, h.vientoKmh());
        assertEquals(22, h.temperaturaC());
        assertEquals(false, h.tormenta());
        assertEquals(0.0, h.precipitacionMm(), 0.0001);
    }

    @Test
    void elEscenarioSoloCubreSusHoras_elLimiteSuperiorNoSeIncluye() {
        fuente.agregar(lluvia(LAT, LON, 5, 7, 90));
        List<HoraPronostico> horas = fuente.obtener(LAT, LON, BASE.plusHours(4), BASE.plusHours(8));
        assertEquals(0, horas.get(0).probLluvia());    // +4
        assertEquals(90, horas.get(1).probLluvia());   // +5
        assertEquals(90, horas.get(2).probLluvia());   // +6
        assertEquals(0, horas.get(3).probLluvia());    // +7 (hasta no se incluye)
    }

    @Test
    void siHayVariosEscenariosGanaElUltimoCargado() {
        fuente.agregar(lluvia(LAT, LON, 0, 10, 40));
        fuente.agregar(lluvia(LAT, LON, 2, 4, 95));
        List<HoraPronostico> horas = fuente.obtener(LAT, LON, BASE, BASE.plusHours(5));
        assertEquals(40, horas.get(0).probLluvia());
        assertEquals(95, horas.get(2).probLluvia());
        assertEquals(40, horas.get(4).probLluvia());
    }

    @Test
    void unEscenarioDeOtroLugarNoAfectaAEstaUbicacion() {
        fuente.agregar(lluvia(-31.4330, -64.1880, 0, 10, 95));   // Parque de las Tejas
        assertEquals(0, fuente.obtener(LAT, LON, BASE, BASE.plusHours(1)).get(0).probLluvia());
        assertEquals(95, fuente.obtener(-31.4330, -64.1880, BASE, BASE.plusHours(1)).get(0).probLluvia());
    }

    @Test
    void borrarTodosVuelveAlTiempoApto() {
        fuente.agregar(lluvia(LAT, LON, 0, 10, 95));
        fuente.borrarTodos();
        assertTrue(fuente.listar().isEmpty());
        assertEquals(0, fuente.obtener(LAT, LON, BASE, BASE.plusHours(1)).get(0).probLluvia());
    }

    @Test
    void losMmDelEscenarioLleganAlPronostico() {
        fuente.agregar(new Escenario(LAT, LON, BASE, BASE.plusHours(2), 30, 10, 18, false, 1.5));
        assertEquals(1.5, fuente.obtener(LAT, LON, BASE, BASE.plusHours(1)).get(0).precipitacionMm(), 0.0001);
    }
}
