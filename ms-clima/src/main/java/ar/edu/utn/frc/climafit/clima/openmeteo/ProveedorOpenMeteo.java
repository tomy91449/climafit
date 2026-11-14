package ar.edu.utn.frc.climafit.clima.openmeteo;

import ar.edu.utn.frc.climafit.clima.modelo.HoraPronostico;
import java.time.LocalDate;
import java.util.List;

/** Puerto hacia el pronóstico externo. Permite probar la caché sin llamar a Internet. */
public interface ProveedorOpenMeteo {

    /** Horas de todos los días entre {@code desde} y {@code hasta} (ambos inclusive). */
    List<HoraPronostico> pronostico(double latitud, double longitud, LocalDate desde, LocalDate hasta);
}
