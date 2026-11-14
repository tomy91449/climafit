package ar.edu.utn.frc.climafit.clima;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ar.edu.utn.frc.climafit.clima.config.ClimaProperties;
import ar.edu.utn.frc.climafit.clima.fuente.FuenteReal;
import ar.edu.utn.frc.climafit.clima.modelo.HoraPronostico;
import ar.edu.utn.frc.climafit.clima.modelo.ModoPronostico;
import ar.edu.utn.frc.climafit.clima.openmeteo.ProveedorOpenMeteo;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Caché de 30 minutos por coordenada y día: se prueba con un proveedor falso (sin Internet). */
class FuenteRealTest {

    private static final double LAT = -31.4290;
    private static final double LON = -64.1800;

    /** Proveedor falso que cuenta las llamadas y devuelve 24 horas por cada día pedido. */
    private static class ProveedorContador implements ProveedorOpenMeteo {
        final List<LocalDate[]> llamadas = new ArrayList<>();

        @Override
        public List<HoraPronostico> pronostico(double lat, double lon, LocalDate desde, LocalDate hasta) {
            llamadas.add(new LocalDate[]{desde, hasta});
            List<HoraPronostico> horas = new ArrayList<>();
            for (LocalDate d = desde; !d.isAfter(hasta); d = d.plusDays(1)) {
                for (int h = 0; h < 24; h++) {
                    horas.add(new HoraPronostico(d.atTime(h, 0), 10, 5, 20, false, 0));
                }
            }
            return horas;
        }
    }

    private final ProveedorContador proveedor = new ProveedorContador();
    private final FuenteReal fuente = new FuenteReal(proveedor,
            new ClimaProperties(ModoPronostico.REAL, "", 30, 2, 5, 10, 10, 2, 0.20));

    @Test
    void laSegundaConsultaDelMismoDiaSale_deLaCache() {
        LocalDateTime desde = LocalDateTime.of(2026, 11, 20, 18, 0);
        fuente.obtener(LAT, LON, desde, desde.plusHours(1));
        fuente.obtener(LAT, LON, desde, desde.plusHours(1));
        assertEquals(1, proveedor.llamadas.size());
    }

    @Test
    void variosDiasFaltantesSePidenJuntosEnUnaSolaLlamada() {
        LocalDateTime desde = LocalDateTime.of(2026, 11, 20, 18, 0);
        List<HoraPronostico> horas = fuente.obtener(LAT, LON, desde, desde.plusDays(3));
        assertEquals(1, proveedor.llamadas.size());
        assertEquals(LocalDate.of(2026, 11, 20), proveedor.llamadas.get(0)[0]);
        assertEquals(LocalDate.of(2026, 11, 23), proveedor.llamadas.get(0)[1]);
        assertEquals(72, horas.size());
    }

    @Test
    void siYaHayUnDiaEnCacheSoloSePidenLosQueFaltan() {
        LocalDateTime d1 = LocalDateTime.of(2026, 11, 20, 18, 0);
        fuente.obtener(LAT, LON, d1, d1.plusHours(1));
        fuente.obtener(LAT, LON, d1, d1.plusDays(1).plusHours(1));
        assertEquals(2, proveedor.llamadas.size());
        assertEquals(LocalDate.of(2026, 11, 21), proveedor.llamadas.get(1)[0]);
        assertEquals(LocalDate.of(2026, 11, 21), proveedor.llamadas.get(1)[1]);
    }

    @Test
    void otraCoordenadaNoUsaLaCacheDeLaPrimera() {
        LocalDateTime desde = LocalDateTime.of(2026, 11, 20, 18, 0);
        fuente.obtener(LAT, LON, desde, desde.plusHours(1));
        fuente.obtener(-31.4330, -64.1880, desde, desde.plusHours(1));
        assertEquals(2, proveedor.llamadas.size());
    }

    @Test
    void devuelveSoloLasHorasQueSeSolapanConLaVentana() {
        LocalDateTime desde = LocalDateTime.of(2026, 11, 20, 18, 0);
        List<HoraPronostico> horas = fuente.obtener(LAT, LON, desde, desde.plusHours(1));
        assertEquals(1, horas.size());
        assertEquals(desde, horas.get(0).fechaHora());
    }
}
