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
 *
 * <p>Para principiantes: un "filtro" es código que corre antes (y después) de cada pedido, antes de
 * llegar al controller. {@link OncePerRequestFilter} garantiza que corra una sola vez por pedido.
 * Es la versión para microservicios (servlet) del CorrelationIdFilter del API Gateway (WebFlux).
 *
 * <p>Lo registra ClimafitTrazabilidadAutoConfiguration, con prioridad alta para que corra primero.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    /** Solo se aceptan identificadores cortos y sin caracteres raros; si no, se genera uno nuevo. */
    private static final Pattern FORMATO_VALIDO = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // 1. Usar el id que vino en la cabecera si es válido; si no vino (o es raro), crear uno nuevo.
        String recibido = request.getHeader(CorrelationId.CABECERA);
        String correlationId = recibido != null && FORMATO_VALIDO.matcher(recibido).matches()
                ? recibido
                : UUID.randomUUID().toString();

        // 2. Guardarlo en el MDC: desde ahora cada línea de log y cada error de este pedido lo incluye.
        MDC.put(CorrelationId.CLAVE_MDC, correlationId);
        // 3. Devolverlo también en la respuesta, para que el cliente lo vea (pestaña Headers en Postman).
        response.setHeader(CorrelationId.CABECERA, correlationId);
        try {
            // 4. Seguir con el pedido (los demás filtros, la seguridad y el controller).
            chain.doFilter(request, response);
        } finally {
            // 5. Al terminar, limpiarlo. El hilo se reutiliza para otro pedido y no debe "heredar" este id.
            MDC.remove(CorrelationId.CLAVE_MDC);
        }
    }
}
