package ar.edu.utn.frc.climafit.clima.openmeteo;

import ar.edu.utn.frc.climafit.clima.modelo.HoraPronostico;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Convierte las listas paralelas de Open-Meteo en {@link HoraPronostico}. */
public final class ConversorPronostico {

    /** Códigos WMO de tormenta eléctrica: 95 (tormenta), 96 y 99 (tormenta con granizo). */
    static final Set<Integer> CODIGOS_TORMENTA = Set.of(95, 96, 99);

    private ConversorPronostico() {
    }

    public static List<HoraPronostico> aHoras(RespuestaOpenMeteo respuesta) {
        RespuestaOpenMeteo.Hourly h = respuesta.hourly();
        List<HoraPronostico> horas = new ArrayList<>();
        if (h == null || h.time() == null) {
            return horas;
        }
        for (int i = 0; i < h.time().size(); i++) {
            horas.add(new HoraPronostico(
                    LocalDateTime.parse(h.time().get(i)),
                    (int) Math.round(valor(h.precipitation_probability(), i)),
                    (int) Math.round(valor(h.wind_speed_10m(), i)),
                    (int) Math.round(valor(h.temperature_2m(), i)),
                    CODIGOS_TORMENTA.contains((int) Math.round(valor(h.weather_code(), i))),
                    valor(h.precipitation(), i)));
        }
        return horas;
    }

    /** Open-Meteo puede devolver null para una hora (por ejemplo la probabilidad): cuenta como 0. */
    private static double valor(List<? extends Number> lista, int i) {
        if (lista == null || i >= lista.size() || lista.get(i) == null) {
            return 0.0;
        }
        return lista.get(i).doubleValue();
    }
}
