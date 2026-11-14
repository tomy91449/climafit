package ar.edu.utn.frc.climafit.clima;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.frc.climafit.clima.evaluacion.EvaluadorClimatico;
import ar.edu.utn.frc.climafit.clima.modelo.CriteriosClimaticos;
import ar.edu.utn.frc.climafit.clima.modelo.EvaluacionVentana;
import ar.edu.utn.frc.climafit.clima.modelo.HoraPronostico;
import ar.edu.utn.frc.climafit.clima.modelo.MargenesRiesgo;
import ar.edu.utn.frc.climafit.clima.modelo.ResultadoClima;
import ar.edu.utn.frc.climafit.clima.modelo.VentanaHoraria;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/** RN10: un test por resultado, uno por criterio y los bordes de los márgenes. */
class EvaluadorClimaticoTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 20, 18, 0);
    private static final VentanaHoraria VENTANA = new VentanaHoraria(INICIO, INICIO.plusHours(1));

    /** Yoga con los criterios originales de la propuesta: 20 %, 25 km/h, 12 °C (sin mm). */
    private static final CriteriosClimaticos YOGA = new CriteriosClimaticos(20, 25, 12, null);

    private final EvaluadorClimatico evaluador = new EvaluadorClimatico(MargenesRiesgo.POR_DEFECTO);

    private static HoraPronostico hora(int lluvia, int viento, int temp, boolean tormenta, double mm) {
        return new HoraPronostico(INICIO, lluvia, viento, temp, tormenta, mm);
    }

    private EvaluacionVentana evaluar(CriteriosClimaticos c, HoraPronostico... horas) {
        return evaluador.evaluar(VENTANA, List.of(horas), c);
    }

    // ---------- un test por resultado ----------

    @Test
    void rn10_aptaCuandoNingunValorEstaCercaDeLosLimites() {
        EvaluacionVentana e = evaluar(YOGA, hora(0, 5, 22, false, 0));
        assertEquals(ResultadoClima.APTA, e.resultado());
        assertEquals("Condiciones aptas para la actividad", e.motivo());
    }

    @Test
    void rn10_enRiesgoCuandoUnValorQuedaCercaDeSuLimite() {
        assertEquals(ResultadoClima.EN_RIESGO, evaluar(YOGA, hora(0, 20, 20, false, 0)).resultado());
    }

    @Test
    void rn10_noAptaCuandoSeSuperaUnLimite() {
        assertEquals(ResultadoClima.NO_APTA, evaluar(YOGA, hora(90, 10, 18, false, 0)).resultado());
    }

    // ---------- un test por criterio ----------

    @Test
    void rn10_noAptaPorProbabilidadDeLluvia() {
        EvaluacionVentana e = evaluar(YOGA, hora(90, 5, 22, false, 0));
        assertEquals(ResultadoClima.NO_APTA, e.resultado());
        assertEquals("Probabilidad de lluvia 90 % (máximo 20 %)", e.motivo());
    }

    @Test
    void rn10_noAptaPorViento() {
        EvaluacionVentana e = evaluar(YOGA, hora(0, 30, 22, false, 0));
        assertEquals(ResultadoClima.NO_APTA, e.resultado());
        assertEquals("Viento 30 km/h (máximo 25 km/h)", e.motivo());
    }

    @Test
    void rn10_noAptaPorTemperaturaMinima() {
        EvaluacionVentana e = evaluar(YOGA, hora(0, 5, 11, false, 0));
        assertEquals(ResultadoClima.NO_APTA, e.resultado());
        assertEquals("Temperatura 11 °C (mínimo 12 °C)", e.motivo());
    }

    @Test
    void rn10_noAptaPorTormentaAunqueTodoLoDemasSeaPerfecto() {
        EvaluacionVentana e = evaluar(YOGA, hora(0, 5, 22, true, 0));
        assertEquals(ResultadoClima.NO_APTA, e.resultado());
        assertEquals("Tormenta eléctrica prevista", e.motivo());
    }

    // ---------- bordes de los márgenes ----------

    @Test
    void rn10_lluviaAExactamente10PuntosDelLimiteEsEnRiesgo() {
        assertEquals(ResultadoClima.EN_RIESGO, evaluar(YOGA, hora(10, 5, 22, false, 0)).resultado());
    }

    @Test
    void rn10_lluviaA11PuntosDelLimiteEsApta() {
        assertEquals(ResultadoClima.APTA, evaluar(YOGA, hora(9, 5, 22, false, 0)).resultado());
    }

    @Test
    void rn10_vientoAExactamente10KmhDelLimiteEsEnRiesgoYA11EsApta() {
        assertEquals(ResultadoClima.EN_RIESGO, evaluar(YOGA, hora(0, 15, 22, false, 0)).resultado());
        assertEquals(ResultadoClima.APTA, evaluar(YOGA, hora(0, 14, 22, false, 0)).resultado());
    }

    @Test
    void rn10_temperaturaA2GradosDelMinimoEsEnRiesgoYA3EsApta() {
        assertEquals(ResultadoClima.EN_RIESGO, evaluar(YOGA, hora(0, 5, 14, false, 0)).resultado());
        assertEquals(ResultadoClima.APTA, evaluar(YOGA, hora(0, 5, 15, false, 0)).resultado());
    }

    @Test
    void rn10_valorIgualAlLimiteNoSupera_esEnRiesgoYNoNoApta() {
        assertEquals(ResultadoClima.EN_RIESGO, evaluar(YOGA, hora(20, 5, 22, false, 0)).resultado());
        assertEquals(ResultadoClima.NO_APTA, evaluar(YOGA, hora(21, 5, 22, false, 0)).resultado());
        assertEquals(ResultadoClima.EN_RIESGO, evaluar(YOGA, hora(0, 5, 12, false, 0)).resultado());
        assertEquals(ResultadoClima.NO_APTA, evaluar(YOGA, hora(0, 5, 11, false, 0)).resultado());
    }

    // ---------- se toma el peor valor de las horas de la clase ----------

    @Test
    void rn10_tomaElPeorValorDeLasHorasDeLaClase() {
        EvaluacionVentana e = evaluar(YOGA,
                hora(5, 5, 22, false, 0),
                hora(18, 24, 13, false, 0));
        assertEquals(ResultadoClima.EN_RIESGO, e.resultado());
        assertEquals(18, e.probLluvia());
        assertEquals(24, e.vientoKmh());
        assertEquals(13, e.temperaturaC());
    }

    @Test
    void rn10_variasCausasSeListanTodas() {
        EvaluacionVentana e = evaluar(YOGA, hora(90, 30, 22, false, 0));
        assertEquals("Probabilidad de lluvia 90 % (máximo 20 %); Viento 30 km/h (máximo 25 km/h)", e.motivo());
    }

    @Test
    void rn10_sinHorasDePronosticoEsUnError() {
        assertThrows(IllegalArgumentException.class, () -> evaluador.evaluar(VENTANA, List.of(), YOGA));
    }

    // ---------- CAMBIO DE DOMINIO: mm de lluvia por hora ----------

    @Test
    void rn10_mmSuperaElMaximoDeLaActividadEsNoApta() {
        CriteriosClimaticos running = new CriteriosClimaticos(60, 40, 2, 2.5);
        EvaluacionVentana e = evaluar(running, hora(30, 5, 20, false, 2.6));
        assertEquals(ResultadoClima.NO_APTA, e.resultado());
        assertEquals("Lluvia 2,6 mm/h (máximo 2,5 mm/h)", e.motivo());
    }

    @Test
    void rn10_mmCercaDelMaximoEsEnRiesgoYLejosEsApta() {
        CriteriosClimaticos running = new CriteriosClimaticos(60, 40, 2, 2.5);
        assertEquals(ResultadoClima.EN_RIESGO, evaluar(running, hora(10, 5, 20, false, 2.0)).resultado());
        assertEquals(ResultadoClima.APTA, evaluar(running, hora(10, 5, 20, false, 1.9)).resultado());
    }

    @Test
    void rn10_mmYProbabilidadSeComplementan_cualquieraQueSupereElLimiteSuspende() {
        CriteriosClimaticos conMm = new CriteriosClimaticos(20, 25, 12, 5.0);
        // mucha probabilidad pero casi nada de agua: igual cuenta la probabilidad
        assertEquals(ResultadoClima.NO_APTA, evaluar(conMm, hora(90, 5, 22, false, 0.1)).resultado());
        // poca probabilidad pero mucha agua: cuentan los mm
        assertEquals(ResultadoClima.NO_APTA, evaluar(conMm, hora(5, 5, 22, false, 6.0)).resultado());
    }

    @Test
    void rn10_actividadQueNoToleraNadaDeAgua_cero_mm_esAptaYCualquierLloviznaNo() {
        CriteriosClimaticos sinAgua = new CriteriosClimaticos(20, 25, 12, 0.0);
        assertEquals(ResultadoClima.APTA, evaluar(sinAgua, hora(0, 5, 22, false, 0.0)).resultado());
        EvaluacionVentana e = evaluar(sinAgua, hora(0, 5, 22, false, 0.1));
        assertEquals(ResultadoClima.NO_APTA, e.resultado());
        assertEquals("Lluvia 0,1 mm/h (máximo 0,0 mm/h)", e.motivo());
    }

    @Test
    void rn10_sinCriterioDeMmNoSeEvaluanLosMm_contratoOriginalDeLaPropuesta() {
        assertEquals(ResultadoClima.APTA, evaluar(YOGA, hora(0, 5, 22, false, 50.0)).resultado());
    }

    @Test
    void rn10_elMaximoDeMmSeTomaPorHora_noSeSumanLasHoras() {
        CriteriosClimaticos running = new CriteriosClimaticos(60, 40, 2, 2.5);
        EvaluacionVentana e = evaluar(running,
                hora(10, 5, 20, false, 1.0),
                hora(10, 5, 20, false, 1.0),
                hora(10, 5, 20, false, 1.0));
        assertEquals(ResultadoClima.APTA, e.resultado());
        assertEquals(1.0, e.precipitacionMm(), 0.0001);
    }

    // ---------- los escenarios de la colección de Postman con los límites provisorios ----------

    @Test
    void rn10_escenariosDePostmanConLosLimitesProvisorios() {
        CriteriosClimaticos yoga = new CriteriosClimaticos(15, 22, 14, 0.0);
        CriteriosClimaticos running = new CriteriosClimaticos(50, 35, 4, 2.5);

        // carpeta 06: 90 % de lluvia, 10 km/h, 18 °C -> NO_APTA
        assertEquals(ResultadoClima.NO_APTA, evaluar(yoga, hora(90, 10, 18, false, 0)).resultado());
        // carpeta 07: viento de 20 km/h, 0 % de lluvia, 20 °C -> EN_RIESGO
        assertEquals(ResultadoClima.EN_RIESGO, evaluar(yoga, hora(0, 20, 20, false, 0)).resultado());
        // carpeta 08: 95 % de lluvia, 15 km/h, 16 °C en Running -> NO_APTA
        assertEquals(ResultadoClima.NO_APTA, evaluar(running, hora(95, 15, 16, false, 0)).resultado());
        // clima apto por defecto del modo SIMULADO -> APTA (el día siguiente de la clase A)
        assertEquals(ResultadoClima.APTA, evaluar(yoga, hora(0, 5, 22, false, 0)).resultado());
        assertTrue(evaluar(running, hora(0, 5, 22, false, 0)).motivo().startsWith("Condiciones aptas"));
    }
}
