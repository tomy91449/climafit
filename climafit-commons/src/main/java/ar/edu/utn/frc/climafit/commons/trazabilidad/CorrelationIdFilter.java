package ar.edu.utn.frc.climafit.commons.trazabilidad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Lee el {@code X-Correlation-Id} del pedido (el gateway o el servicio que llama lo envía) o genera
 * uno nuevo, lo guarda en el MDC para que aparezca en los logs y en los errores, y lo devuelve
 * en la respuesta.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    /** Solo se aceptan identificadores cortos y sin caracteres raros; si no, se genera uno nuevo. */
    private static final Pattern FORMATO_VALIDO = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String recibido = request.getHeader(CorrelationId.CABECERA);
        String correlationId = recibido != null && FORMATO_VALIDO.matcher(recibido).matches()
                ? recibido
                : UUID.randomUUID().toString();

        MDC.put(CorrelationId.CLAVE_MDC, correlationId);
        response.setHeader(CorrelationId.CABECERA, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(CorrelationId.CLAVE_MDC);
        }
    }
}
