package ar.edu.utn.frc.climafit.gateway;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * El gateway es donde "nace" el X-Correlation-Id: si la solicitud no lo trae, se genera uno,
 * se reenvía al microservicio y se devuelve al cliente. Con ese id se sigue la solicitud en
 * los logs de todos los servicios.
 * <p>
 * Es un WebFilter (y no un GlobalFilter de Gateway) para que corra ANTES que el Security Filter:
 * así hasta un 401 del gateway lleva su correlation id.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements WebFilter {

    public static final String HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String id = exchange.getRequest().getHeaders().getFirst(HEADER);
        String correlationId = (id == null || id.isBlank()) ? UUID.randomUUID().toString() : id;

        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(h -> h.set(HEADER, correlationId))
                .build();
        exchange.getResponse().getHeaders().set(HEADER, correlationId);
        return chain.filter(exchange.mutate().request(request).build());
    }
}
