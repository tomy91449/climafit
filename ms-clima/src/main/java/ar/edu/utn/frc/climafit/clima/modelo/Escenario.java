package ar.edu.utn.frc.climafit.clima.modelo;

import java.time.LocalDateTime;

/** Escenario de clima simulado para una ubicación y un período (solo perfil demo). */
public record Escenario(double latitud, double longitud, LocalDateTime desde, LocalDateTime hasta,
                        int probLluvia, int vientoKmh, int temperaturaC, boolean tormenta,
                        double precipitacionMm) {
}
