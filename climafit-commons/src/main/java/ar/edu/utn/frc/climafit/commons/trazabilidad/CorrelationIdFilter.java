package ar.edu.utn.frc.climafit.commons.trazabilidad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Asigna un identificador a cada solicitud para poder seguir un proceso distribuido en los logs.
 * <p>
 * El gateway genera el {@code X-Correlation-Id}; cada servicio lo lee, lo pone en el MDC
 * (así aparece en cada línea de log) y lo devuelve en la respuesta. Cuando ms-clases llame a
 * ms-clima, va a reenviar el mismo id: buscando ese valor en los logs de todos los contenedores
 * se ve el recorrido completo de una evaluación.
 * <p>
 * Si la solicitud llega sin id (por ejemplo, la tarea programada), se genera uno nuevo.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = request.getHeader(HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            // Los hilos del servidor se reutilizan: si no se limpia, el id "se filtra" a la próxima solicitud.
            MDC.remove(MDC_KEY);
        }
    }

    /** Id de la solicitud actual (o null fuera de una solicitud HTTP). */
    public static String actual() {
        return MDC.get(MDC_KEY);
    }
}
