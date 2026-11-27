package ar.edu.utn.frc.climafit.commons.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Arma los {@link ErrorResponse} con la ruta del pedido, la fecha actual y el correlation id
 * que dejó el filtro de trazabilidad en el MDC.
 *
 * <p>Para principiantes: una "fábrica" (factory) es una clase cuyo trabajo es crear objetos.
 * Así, quien necesita un error solo dice status, código y mensaje; lo demás (ruta, fecha y
 * correlation id) se completa acá, siempre de la misma forma.
 *
 * <p>El MDC (Mapped Diagnostic Context) es un "bolsillo" de datos propio de cada pedido que usa el
 * sistema de logs. CorrelationIdFilter guarda ahí el correlation id al principio del pedido y acá se lee.
 */
public final class ErrorResponseFactory {

    /** Clave del MDC donde el filtro de trazabilidad guarda el correlation id. */
    public static final String MDC_CORRELATION_ID = "correlationId";

    // Constructor privado: solo tiene métodos estáticos, se usa como ErrorResponseFactory.crear(...).
    private ErrorResponseFactory() {
    }

    /** Error sin detalle de campos (401, 403, 404, 409, 503...). */
    public static ErrorResponse crear(int status, String codigo, String mensaje, HttpServletRequest request) {
        return crear(status, codigo, mensaje, request, null);
    }

    /** Error con detalle de campos (400 de validación: qué campo falló y por qué). */
    public static ErrorResponse crear(int status, String codigo, String mensaje, HttpServletRequest request,
                                      Map<String, String> campos) {
        // La URL pedida, por ejemplo "/clases/7/inscripciones".
        String ruta = request != null ? request.getRequestURI() : null;
        // withNano(0): la fecha sin milisegundos, más fácil de leer.
        return new ErrorResponse(status, codigo, mensaje, ruta, MDC.get(MDC_CORRELATION_ID),
                LocalDateTime.now().withNano(0), campos);
    }
}
