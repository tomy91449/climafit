package ar.edu.utn.frc.climafit.clima;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.utn.frc.climafit.clima.config.ClimaProperties;
import ar.edu.utn.frc.climafit.clima.evaluacion.EvaluacionClimaService;
import ar.edu.utn.frc.climafit.clima.evaluacion.EvaluadorClimatico;
import ar.edu.utn.frc.climafit.clima.fuente.FuenteFalla;
import ar.edu.utn.frc.climafit.clima.fuente.FuentePronostico;
import ar.edu.utn.frc.climafit.clima.fuente.FuenteSimulada;
import ar.edu.utn.frc.climafit.clima.fuente.SelectorFuente;
import ar.edu.utn.frc.climafit.clima.modelo.CriteriosClimaticos;
import ar.edu.utn.frc.climafit.clima.modelo.Escenario;
import ar.edu.utn.frc.climafit.clima.modelo.EvaluacionVentana;
import ar.edu.utn.frc.climafit.clima.modelo.HoraPronostico;
import ar.edu.utn.frc.climafit.clima.modelo.MargenesRiesgo;
import ar.edu.utn.frc.climafit.clima.modelo.ModoPronostico;
import ar.edu.utn.frc.climafit.clima.modelo.ResultadoClima;
import ar.edu.utn.frc.climafit.clima.modelo.VentanaHoraria;
import ar.edu.utn.frc.climafit.commons.error.NegocioException;
import ar.edu.utn.frc.climafit.commons.error.ServicioNoDisponibleException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EvaluacionClimaServiceTest {

    private static final double LAT = -31.4290;
    private static final double LON = -64.1800;
    private static final LocalDateTime CLASE = LocalDateTime.of(2026, 11, 20, 18, 0);
    private static final CriteriosClimaticos YOGA = new CriteriosClimaticos(20, 25, 12, null);

    private static ClimaProperties props(ModoPronostico modo) {
        return new ClimaProperties(modo, "", 30, 2, 5, 10, 10, 2, 0.20);
    }

    private static EvaluacionClimaService servicio(ModoPronostico modo, FuentePronostico... fuentes) {
        return new EvaluacionClimaService(new SelectorFuente(List.of(fuentes), props(modo)),
                new EvaluadorClimatico(MargenesRiesgo.POR_DEFECTO));
    }

    private static VentanaHoraria ventana(LocalDateTime inicio) {
        return new VentanaHoraria(inicio, inicio.plusHours(1));
    }

    @Test
    void rn16_enModoFallaElServicioNoEstaDisponible() {
        EvaluacionClimaService s = servicio(ModoPronostico.FALLA, new FuenteSimulada(), new FuenteFalla());
        assertThrows(ServicioNoDisponibleException.class,
                () -> s.evaluar(LAT, LON, YOGA, List.of(ventana(CLASE))));
    }

    @Test
    void rn16_siElProveedorNoTieneHorasParaLaVentanaResponde503() {
        FuentePronostico vacia = new FuentePronostico() {
            public ModoPronostico modo() { return ModoPronostico.SIMULADO; }
            public List<HoraPronostico> obtener(double a, double b, LocalDateTime d, LocalDateTime h) {
                return List.of();
            }
        };
        EvaluacionClimaService s = servicio(ModoPronostico.SIMULADO, vacia);
        assertThrows(ServicioNoDisponibleException.class,
                () -> s.evaluar(LAT, LON, YOGA, List.of(ventana(CLASE))));
    }

    @Test
    void variasVentanasSeEvaluanConUnaSolaConsultaYEnElMismoOrden() {
        FuenteSimulada sim = new FuenteSimulada();
        // lluvia a las 18 del día +2 y del día +3; los demás días, clima apto por defecto
        sim.agregar(new Escenario(LAT, LON, CLASE.plusDays(2), CLASE.plusDays(2).plusHours(1), 90, 10, 18, false, 0));
        sim.agregar(new Escenario(LAT, LON, CLASE.plusDays(3), CLASE.plusDays(3).plusHours(1), 90, 10, 18, false, 0));

        List<Integer> consultas = new ArrayList<>();
        FuentePronostico contador = new FuentePronostico() {
            public ModoPronostico modo() { return ModoPronostico.SIMULADO; }
            public List<HoraPronostico> obtener(double a, double b, LocalDateTime d, LocalDateTime h) {
                consultas.add(1);
                return sim.obtener(a, b, d, h);
            }
        };
        EvaluacionClimaService s = servicio(ModoPronostico.SIMULADO, contador);

        List<VentanaHoraria> ventanas = new ArrayList<>();
        for (int d = 1; d <= 7; d++) {
            ventanas.add(ventana(CLASE.plusDays(d)));
        }
        List<EvaluacionVentana> r = s.evaluar(LAT, LON, YOGA, ventanas);

        assertEquals(1, consultas.size());
        assertEquals(7, r.size());
        assertEquals(ResultadoClima.APTA, r.get(0).resultado());      // día +1
        assertEquals(ResultadoClima.NO_APTA, r.get(1).resultado());   // día +2
        assertEquals(ResultadoClima.NO_APTA, r.get(2).resultado());   // día +3
        assertEquals(ResultadoClima.APTA, r.get(3).resultado());      // día +4
        for (int i = 0; i < 7; i++) {
            assertEquals(CLASE.plusDays(i + 1), r.get(i).inicio());
        }
    }

    @Test
    void unaVentanaQueTerminaAntesDeEmpezarEsUnDatoInvalido() {
        EvaluacionClimaService s = servicio(ModoPronostico.SIMULADO, new FuenteSimulada());
        NegocioException e = assertThrows(NegocioException.class,
                () -> s.evaluar(LAT, LON, YOGA, List.of(new VentanaHoraria(CLASE, CLASE.minusHours(1)))));
        assertEquals("DATOS_INVALIDOS", e.getCodigo());
    }
}
