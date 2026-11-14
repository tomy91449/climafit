package ar.edu.utn.frc.climafit.clases.reprogramacion;

import java.time.LocalDateTime;

/**
 * Resultado de buscar un nuevo día (RN13), sin haber cambiado nada todavía.
 *
 * @param nuevoInicio el primer día viable, o null si no hay ninguno
 */
public record PlanReprogramacion(LocalDateTime nuevoInicio) {

    public boolean hayDia() {
        return nuevoInicio != null;
    }
}
