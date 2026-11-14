package ar.edu.utn.frc.climafit.clima.fuente;

import ar.edu.utn.frc.climafit.clima.modelo.HoraPronostico;
import ar.edu.utn.frc.climafit.clima.modelo.ModoPronostico;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Estrategia de pronóstico: hay una implementación por modo (REAL, SIMULADO, FALLA).
 * El resto del servicio no sabe de dónde salen las horas.
 */
public interface FuentePronostico {

    ModoPronostico modo();

    /**
     * Horas cuyo bloque se solapa con [desde, hasta).
     *
     * @throws ar.edu.utn.frc.climafit.commons.error.ServicioNoDisponibleException si no hay pronóstico
     */
    List<HoraPronostico> obtener(double latitud, double longitud, LocalDateTime desde,
                                 LocalDateTime hasta);
}
