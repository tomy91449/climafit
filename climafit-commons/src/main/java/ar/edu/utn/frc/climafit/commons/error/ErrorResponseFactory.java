package ar.edu.utn.frc.climafit.commons.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Arma los {@link ErrorResponse} con la ruta del pedido, la fecha actual y el correlation id
 * que dejó el filtro de trazabilidad en el MDC.
 */
public final class ErrorResponseFactory {

    /** Clave del MDC donde el filtro de trazabilidad guarda el correlation id. */
    public static final String MDC_CORRELATION_ID = "correlationId";

    private ErrorResponseFactory() {
    }

    public static ErrorResponse crear(int status, String codigo, String mensaje, HttpServletRequest request) {
        return crear(status, codigo, mensaje, request, null);
    }

    public static ErrorResponse crear(int status, String codigo, String mensaje, HttpServletRequest request,
                                      Map<String, String> campos) {
        String ruta = request != null ? request.getRequestURI() : null;
        return new ErrorResponse(status, codigo, mensaje, ruta, MDC.get(MDC_CORRELATION_ID),
                LocalDateTime.now().withNano(0), campos);
    }
}
