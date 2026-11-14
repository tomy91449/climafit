package ar.edu.utn.frc.climafit.clima.evaluacion;

import ar.edu.utn.frc.climafit.clima.modelo.CriteriosClimaticos;
import ar.edu.utn.frc.climafit.clima.modelo.EvaluacionVentana;
import ar.edu.utn.frc.climafit.clima.modelo.HoraPronostico;
import ar.edu.utn.frc.climafit.clima.modelo.MargenesRiesgo;
import ar.edu.utn.frc.climafit.clima.modelo.ResultadoClima;
import ar.edu.utn.frc.climafit.clima.modelo.VentanaHoraria;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RN10: decide si una ventana horaria es APTA, EN_RIESGO o NO_APTA para una actividad.
 *
 * <p>Es lógica pura (sin Spring, sin red): recibe las horas de la clase y los criterios, y
 * devuelve el veredicto con un motivo en español. Se toma el peor valor de las horas de la
 * clase: mayor probabilidad de lluvia, mayor viento, menor temperatura, mayor lluvia por hora
 * y si hay tormenta eléctrica en alguna.
 *
 * <p>CAMBIO DE DOMINIO (chat del 12/11): los mm de lluvia por hora se EVALÚAN ADEMÁS de la
 * probabilidad. Son criterios independientes: superar cualquiera de los dos es NO_APTA.
 */
public class EvaluadorClimatico {

    /** Tolerancia para comparar decimales (mm) sin errores de redondeo de punto flotante. */
    private static final double EPS = 1e-9;

    private final MargenesRiesgo margenes;

    public EvaluadorClimatico(MargenesRiesgo margenes) {
        this.margenes = margenes;
    }

    public EvaluacionVentana evaluar(VentanaHoraria ventana, List<HoraPronostico> horas,
                                     CriteriosClimaticos c) {
        if (horas.isEmpty()) {
            throw new IllegalArgumentException("La ventana " + ventana + " no tiene horas de pronóstico");
        }
        int lluvia = horas.stream().mapToInt(HoraPronostico::probLluvia).max().getAsInt();
        int viento = horas.stream().mapToInt(HoraPronostico::vientoKmh).max().getAsInt();
        int temperatura = horas.stream().mapToInt(HoraPronostico::temperaturaC).min().getAsInt();
        double mm = horas.stream().mapToDouble(HoraPronostico::precipitacionMm).max().getAsDouble();
        boolean tormenta = horas.stream().anyMatch(HoraPronostico::tormenta);

        List<String> causasNoApta = new ArrayList<>();
        boolean usaMm = c.lluviaMaxMmHora() != null;

        // RN10 - NO_APTA: se supera algún máximo, baja del mínimo o hay tormenta eléctrica.
        if (lluvia > c.probLluviaMax()) {
            causasNoApta.add("Probabilidad de lluvia " + lluvia + " % (máximo " + c.probLluviaMax() + " %)");
        }
        if (viento > c.vientoMaxKmh()) {
            causasNoApta.add("Viento " + viento + " km/h (máximo " + c.vientoMaxKmh() + " km/h)");
        }
        if (temperatura < c.tempMinC()) {
            causasNoApta.add("Temperatura " + temperatura + " °C (mínimo " + c.tempMinC() + " °C)");
        }
        if (usaMm && mm > c.lluviaMaxMmHora() + EPS) {
            causasNoApta.add("Lluvia " + mmTexto(mm) + " mm/h (máximo " + mmTexto(c.lluviaMaxMmHora()) + " mm/h)");
        }
        if (tormenta) {
            causasNoApta.add("Tormenta eléctrica prevista");
        }
        if (!causasNoApta.isEmpty()) {
            return resultado(ventana, ResultadoClima.NO_APTA, String.join("; ", causasNoApta),
                    lluvia, viento, temperatura, tormenta, mm);
        }

        // RN10 - EN_RIESGO: ningún límite se supera, pero algún valor queda cerca.
        List<String> causasRiesgo = new ArrayList<>();
        if (c.probLluviaMax() - lluvia <= margenes.lluviaPuntos()) {
            causasRiesgo.add("Probabilidad de lluvia " + lluvia + " % cerca del máximo (" + c.probLluviaMax() + " %)");
        }
        if (c.vientoMaxKmh() - viento <= margenes.vientoKmh()) {
            causasRiesgo.add("Viento " + viento + " km/h cerca del máximo (" + c.vientoMaxKmh() + " km/h)");
        }
        if (temperatura - c.tempMinC() <= margenes.temperaturaC()) {
            causasRiesgo.add("Temperatura " + temperatura + " °C cerca del mínimo (" + c.tempMinC() + " °C)");
        }
        // Con límite 0 (la actividad no tolera nada de agua) no hay zona de riesgo: 0 mm es APTA.
        if (usaMm && c.lluviaMaxMmHora() > 0
                && mm + EPS >= c.lluviaMaxMmHora() * (1 - margenes.lluviaMmProporcion())) {
            causasRiesgo.add("Lluvia " + mmTexto(mm) + " mm/h cerca del máximo (" + mmTexto(c.lluviaMaxMmHora()) + " mm/h)");
        }
        if (!causasRiesgo.isEmpty()) {
            return resultado(ventana, ResultadoClima.EN_RIESGO, String.join("; ", causasRiesgo),
                    lluvia, viento, temperatura, tormenta, mm);
        }

        return resultado(ventana, ResultadoClima.APTA, "Condiciones aptas para la actividad",
                lluvia, viento, temperatura, tormenta, mm);
    }

    private static EvaluacionVentana resultado(VentanaHoraria v, ResultadoClima r, String motivo,
                                               int lluvia, int viento, int temperatura,
                                               boolean tormenta, double mm) {
        return new EvaluacionVentana(v.inicio(), r, motivo, lluvia, viento, temperatura, tormenta,
                Math.round(mm * 100) / 100.0);
    }

    private static String mmTexto(double valor) {
        return String.format(Locale.forLanguageTag("es-AR"), "%.1f", valor);
    }
}
