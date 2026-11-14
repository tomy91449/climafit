package ar.edu.utn.frc.climafit.clima.fuente;

import ar.edu.utn.frc.climafit.clima.config.ClimaProperties;
import ar.edu.utn.frc.climafit.clima.modelo.HoraPronostico;
import ar.edu.utn.frc.climafit.clima.modelo.HorasDeVentana;
import ar.edu.utn.frc.climafit.clima.modelo.ModoPronostico;
import ar.edu.utn.frc.climafit.clima.openmeteo.ProveedorOpenMeteo;
import ar.edu.utn.frc.climafit.commons.error.ServicioNoDisponibleException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Modo REAL: Open-Meteo con caché en memoria (Caffeine, 30 minutos por coordenada y día,
 * propuesta sección 08). Si faltan días en la caché, los pide todos juntos en una sola llamada.
 */
@Component
public class FuenteReal implements FuentePronostico {

    /** Las coordenadas se redondean a 4 decimales (~11 m) para armar la clave de caché. */
    private record ClaveDia(double latitud, double longitud, LocalDate dia) {
    }

    private final ProveedorOpenMeteo proveedor;
    private final Cache<ClaveDia, List<HoraPronostico>> cache;

    public FuenteReal(ProveedorOpenMeteo proveedor, ClimaProperties props) {
        this.proveedor = proveedor;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(props.cacheMinutos(), TimeUnit.MINUTES)
                .maximumSize(2_000)
                .build();
    }

    @Override
    public ModoPronostico modo() {
        return ModoPronostico.REAL;
    }

    /**
     * Devuelve las horas pedidas consultando solo los días que no están en caché y respetando
     * el intervalo solicitado. Así se reutiliza el pronóstico sin devolver horas ajenas a la ventana.
     */
    @Override
    public List<HoraPronostico> obtener(double latitud, double longitud, LocalDateTime desde,
                                        LocalDateTime hasta) {
        LocalDate primero = desde.toLocalDate();
        LocalDate ultimo = hasta.minusSeconds(1).toLocalDate();   // "hasta" no se incluye
        List<LocalDate> dias = primero.datesUntil(ultimo.plusDays(1)).toList();

        List<LocalDate> faltantes = dias.stream()
                .filter(d -> cache.getIfPresent(clave(latitud, longitud, d)) == null)
                .toList();
        if (!faltantes.isEmpty()) {
            cargar(latitud, longitud, faltantes.get(0), faltantes.get(faltantes.size() - 1));
        }

        List<HoraPronostico> horas = new ArrayList<>();
        for (LocalDate dia : dias) {
            List<HoraPronostico> delDia = cache.getIfPresent(clave(latitud, longitud, dia));
            if (delDia == null) {
                throw new ServicioNoDisponibleException("No hay pronóstico para el día " + dia);
            }
            horas.addAll(delDia);
        }
        return HorasDeVentana.filtrar(horas, desde, hasta);
    }

    /** Consulta Open-Meteo una vez por el tramo faltante y guarda las horas agrupadas por día. */
    private void cargar(double lat, double lon, LocalDate desde, LocalDate hasta) {
        List<HoraPronostico> recibidas = proveedor.pronostico(lat, lon, desde, hasta);
        Map<LocalDate, List<HoraPronostico>> porDia = recibidas.stream()
                .collect(Collectors.groupingBy(h -> h.fechaHora().toLocalDate(), TreeMap::new,
                        Collectors.toList()));
        porDia.forEach((dia, horas) -> cache.put(clave(lat, lon, dia), horas));
    }

    /** Construye la clave estable de caché usando coordenadas redondeadas y fecha local. */
    private static ClaveDia clave(double lat, double lon, LocalDate dia) {
        return new ClaveDia(Math.round(lat * 10_000) / 10_000.0, Math.round(lon * 10_000) / 10_000.0, dia);
    }
}
