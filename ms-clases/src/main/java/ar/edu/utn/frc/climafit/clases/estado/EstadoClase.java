package ar.edu.utn.frc.climafit.clases.estado;

/**
 * PROVISORIO (Persona 3, tarea 3.2): estados y transiciones de la propuesta, sección 4.2.
 * EN_RIESGO vuelve a su estado_previo, que se resuelve en CambioEstadoService.
 */
public enum EstadoClase {
    PROGRAMADA, EN_RIESGO, SUSPENDIDA, REPROGRAMADA, SUSPENDIDA_SIN_FECHA, CANCELADA;

    /** Transición permitida sin considerar el estado previo (EN_RIESGO -> previo se valida aparte). */
    public boolean permiteIrA(EstadoClase nuevo) {
        return switch (this) {
            case PROGRAMADA -> nuevo == EN_RIESGO || nuevo == SUSPENDIDA || nuevo == REPROGRAMADA || nuevo == CANCELADA;
            case REPROGRAMADA -> nuevo == EN_RIESGO || nuevo == SUSPENDIDA || nuevo == REPROGRAMADA || nuevo == CANCELADA;
            case EN_RIESGO -> nuevo == SUSPENDIDA || nuevo == CANCELADA;
            case SUSPENDIDA -> nuevo == REPROGRAMADA || nuevo == SUSPENDIDA_SIN_FECHA || nuevo == CANCELADA;
            case SUSPENDIDA_SIN_FECHA -> nuevo == REPROGRAMADA || nuevo == CANCELADA;
            case CANCELADA -> false;
        };
    }
}
