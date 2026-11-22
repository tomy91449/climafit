package ar.edu.utn.frc.climafit.gateway.trazabilidad;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * El gateway es donde "nace" el X-Correlation-Id: si la solicitud no lo trae (o lo trae con un
 * formato inválido), se genera uno, se reenvía al microservicio y se devuelve al cliente.
 * Con ese id se sigue la solicitud en los logs de todos los servicios.
 * <p>
 * Acepta el mismo formato que el filtro de climafit-commons, así un cliente no puede meter
 * texto arbitrario (saltos de línea, etc.) en los logs de los microservicios.
 * <p>
 * Es un WebFilter (y no un GlobalFilter de Gateway) para que corra ANTES que el Security Filter:
 * así hasta un 401 del gateway lleva su correlation id.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements WebFilter {

    public static final String CABECERA = "X-Correlation-Id";

    /** Atributo del exchange donde queda el id, para los errores que arma el gateway. */
    private static final String ATRIBUTO = CorrelationIdFilter.class.getName();

    /** El mismo patrón que CorrelationIdFilter de climafit-commons. */
    private static final Pattern FORMATO_VALIDO = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String recibido = exchange.getRequest().getHeaders().getFirst(CABECERA);
        String correlationId = recibido != null && FORMATO_VALIDO.matcher(recibido).matches()
                ? recibido
                : UUID.randomUUID().toString();

        exchange.getAttributes().put(ATRIBUTO, correlationId);
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(h -> h.set(CABECERA, correlationId))
                .build();
        exchange.getResponse().getHeaders().set(CABECERA, correlationId);
        return chain.filter(exchange.mutate().request(request).build());
    }

    /** El correlation id de la solicitud, o {@code null} si todavía no pasó por este filtro. */
    public static String de(ServerWebExchange exchange) {
        String id = exchange.getAttribute(ATRIBUTO);
        return id != null ? id : exchange.getResponse().getHeaders().getFirst(CABECERA);
    }
}
