package ar.edu.utn.frc.climafit.commons.trazabilidad;

import ar.edu.utn.frc.climafit.commons.error.ErrorResponseFactory;
import org.slf4j.MDC;

/**
 * Identificador que acompaña a un pedido por todos los servicios que participan en él
 * (cabecera {@code X-Correlation-Id}). Permite seguir un proceso distribuido en los logs.
 */
public final class CorrelationId {

    /** Cabecera HTTP en la que viaja el identificador. */
    public static final String CABECERA = "X-Correlation-Id";

    /** Clave del MDC (la misma que lee {@link ErrorResponseFactory}). */
    public static final String CLAVE_MDC = ErrorResponseFactory.MDC_CORRELATION_ID;

    private CorrelationId() {
    }

    /** El correlation id del pedido que se está atendiendo, o {@code null} si no hay. */
    public static String actual() {
        return MDC.get(CLAVE_MDC);
    }
}
