package ar.edu.utn.frc.climafit.gateway.error;

import ar.edu.utn.frc.climafit.commons.error.ErrorResponse;
import ar.edu.utn.frc.climafit.gateway.trazabilidad.CorrelationIdFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Escribe las respuestas de error del gateway en el formato común ({@link ErrorResponse}).
 * La usan el Security Filter (401/403), el bloqueo de rutas internas (404) y el manejador
 * de errores del gateway (404 de ruta inexistente, 503 de microservicio caído).
 * <p>
 * Explicado simple: cuando el gateway tiene que decir "no", lo dice siempre con el mismo JSON que usan
 * los microservicios. {@code ErrorResponse} viene de climafit-commons, así el formato es uno solo:
 * <pre>
 * {"status": 401, "codigo": "NO_AUTENTICADO", "mensaje": "Se requiere un token válido",
 *  "ruta": "/api/clases", "correlationId": "8f3c2a...", "fecha": "2026-11-24T18:00:00"}
 * </pre>
 */
@Component
public class RespuestasError {

    /** Convierte objetos Java a texto JSON. */
    private final ObjectMapper objectMapper;

    public RespuestasError(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Responde el error: pone el código HTTP (401, 403, 404, 503...), las cabeceras y el JSON.
     *
     * @param status  código HTTP de la respuesta
     * @param codigo  código de error de ClimaFit (de {@code CodigosError} de climafit-commons)
     * @param mensaje texto para el usuario, en español
     */
    public Mono<Void> escribir(ServerWebExchange exchange, HttpStatus status, String codigo, String mensaje) {
        ServerHttpResponse response = exchange.getResponse();
        String correlationId = CorrelationIdFilter.de(exchange);

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        if (correlationId != null) {
            response.getHeaders().set(CorrelationIdFilter.CABECERA, correlationId);
        }
        if (status == HttpStatus.UNAUTHORIZED) {
            // Propuesta, sección 10.1: el 401 lleva WWW-Authenticate: Bearer
            // (es el estándar: le indica al cliente que tiene que mandar un token "Bearer").
            response.getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }

        ErrorResponse cuerpo = new ErrorResponse(status.value(), codigo, mensaje,
                exchange.getRequest().getPath().value(), correlationId, LocalDateTime.now().withNano(0));
        try {
            DataBuffer buffer = response.bufferFactory().wrap(objectMapper.writeValueAsBytes(cuerpo));
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException ex) {
            return Mono.error(ex);
        }
    }
}
