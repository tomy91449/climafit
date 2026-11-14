package ar.edu.utn.frc.climafit.clases.estado;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Estados de una clase y sus transiciones permitidas (propuesta, sección 4.2).
 * Una máquina de estados es una tabla de "desde este estado solo se puede ir a estos otros".
 */
public enum EstadoClase {
    PROGRAMADA,
    EN_RIESGO,
    SUSPENDIDA,
    REPROGRAMADA,
    SUSPENDIDA_SIN_FECHA,
    CANCELADA;

    /** Estados en los que la clase sigue "vigente": tiene fecha y se va a dar (RN02, RN03, RN04). */
    public static final List<EstadoClase> VIGENTES = List.of(PROGRAMADA, EN_RIESGO, REPROGRAMADA);

    private static final Map<EstadoClase, Set<EstadoClase>> TRANSICIONES = new EnumMap<>(EstadoClase.class);

    static {
        TRANSICIONES.put(PROGRAMADA, EnumSet.of(EN_RIESGO, SUSPENDIDA, REPROGRAMADA, CANCELADA));
        // REPROGRAMADA -> REPROGRAMADA: otro cambio de fecha
        TRANSICIONES.put(REPROGRAMADA, EnumSet.of(EN_RIESGO, SUSPENDIDA, REPROGRAMADA, CANCELADA));
        // EN_RIESGO -> su estado_previo se resuelve aparte, porque depende de la clase
        TRANSICIONES.put(EN_RIESGO, EnumSet.of(SUSPENDIDA, CANCELADA));
        TRANSICIONES.put(SUSPENDIDA, EnumSet.of(REPROGRAMADA, SUSPENDIDA_SIN_FECHA, CANCELADA));
        TRANSICIONES.put(SUSPENDIDA_SIN_FECHA, EnumSet.of(REPROGRAMADA, CANCELADA));
        TRANSICIONES.put(CANCELADA, EnumSet.noneOf(EstadoClase.class));
    }

    /**
     * @param nuevo         estado al que se quiere pasar
     * @param estadoPrevio  estado_previo de la clase (solo importa si el estado actual es EN_RIESGO)
     */
    public boolean permiteTransicionA(EstadoClase nuevo, EstadoClase estadoPrevio) {
        if (this == EN_RIESGO && nuevo != null && nuevo == estadoPrevio) {
            return true;
        }
        return TRANSICIONES.get(this).contains(nuevo);
    }

    public boolean esVigente() {
        return VIGENTES.contains(this);
    }
}
