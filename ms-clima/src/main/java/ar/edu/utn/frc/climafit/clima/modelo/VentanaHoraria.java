package ar.edu.utn.frc.climafit.clima.modelo;

import java.time.LocalDateTime;

/** Intervalo [inicio, fin] de una clase (o de un día candidato de reprogramación). */
public record VentanaHoraria(LocalDateTime inicio, LocalDateTime fin) {
}
