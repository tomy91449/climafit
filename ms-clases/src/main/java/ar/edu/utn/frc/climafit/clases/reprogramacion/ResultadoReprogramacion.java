package ar.edu.utn.frc.climafit.clases.reprogramacion;

import ar.edu.utn.frc.climafit.clases.estado.EstadoClase;
import java.time.LocalDateTime;

/** Cómo terminó la clase después de suspenderse: REPROGRAMADA (con fecha) o SUSPENDIDA_SIN_FECHA. */
public record ResultadoReprogramacion(EstadoClase estadoFinal, LocalDateTime nuevoInicio) {
}
