package ar.edu.utn.frc.climafit.clima.modelo;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Regla única para decidir qué horas del pronóstico "pertenecen" a una ventana.
 * Un bloque horario [h, h+1) cuenta si se solapa con [inicio, fin). Una clase de 18:00 a 19:00
 * usa solo el bloque de las 18; una de 18:30 a 19:30 usa los de las 18 y las 19.
 */
public final class HorasDeVentana {

    private HorasDeVentana() {
    }

    public static boolean cubre(LocalDateTime bloque, LocalDateTime inicio, LocalDateTime fin) {
        return bloque.isBefore(fin) && bloque.plusHours(1).isAfter(inicio);
    }

    public static List<HoraPronostico> filtrar(List<HoraPronostico> horas, LocalDateTime inicio,
                                               LocalDateTime fin) {
        return horas.stream().filter(h -> cubre(h.fechaHora(), inicio, fin)).toList();
    }

    public static LocalDateTime truncarAHora(LocalDateTime t) {
        return t.truncatedTo(ChronoUnit.HOURS);
    }
}
