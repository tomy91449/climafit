package ar.edu.utn.frc.climafit.gateway.error;

import ar.edu.utn.frc.climafit.commons.error.CodigosError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Errores que produce el propio gateway, en el formato común (como el ManejadorGlobalErrores de
 * climafit-commons en los microservicios):
 * <ul>
 *   <li>ruta que no existe -> 404 NO_ENCONTRADO;</li>
 *   <li>microservicio caído, que no responde o tarda más que el timeout -> 503 SERVICIO_NO_DISPONIBLE
 *       (propuesta, sección 06);</li>
 *   <li>cualquier otra cosa -> 500 ERROR_INTERNO, sin exponer el detalle.</li>
 * </ul>
 * Corre antes que el manejador por defecto de Spring Boot (orden -1).
 * <p>
 * Explicado simple: es la "red de seguridad" del gateway. Si al reenviar un pedido algo sale mal
 * (por ejemplo, ms-clases está apagado), en vez de devolver un error técnico confuso, responde un
 * 503 claro: "El servicio ms-clases no está disponible. Intentá de nuevo en unos minutos". Así una
 * falla de un microservicio no rompe todo el sistema: los demás siguen funcionando.
 */
@Component
@Order(-2)
public class ManejadorErroresGateway implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErroresGateway.class);

    private final RespuestasError respuestas;

    public ManejadorErroresGateway(RespuestasError respuestas) {
        this.respuestas = respuestas;
    }

    /** Spring llama a este método cada vez que un pedido termina en error dentro del gateway. */
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        // Si ya se empezó a mandar la respuesta, no se puede cambiar.
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }
        // Caso 1: no se pudo hablar con el microservicio (apagado, no responde, tardó demasiado) -> 503.
        if (esFallaDeConexion(ex)) {
            String servicio = servicioDestino(exchange);
            log.warn("{} {} -> {} no disponible: {}", exchange.getRequest().getMethod(),
                    exchange.getRequest().getPath(), servicio, ex.toString());
            return respuestas.escribir(exchange, HttpStatus.SERVICE_UNAVAILABLE, CodigosError.SERVICIO_NO_DISPONIBLE,
                    "El servicio " + servicio + " no está disponible. Intentá de nuevo en unos minutos");
        }
        // Caso 2: un error con código HTTP conocido (404, 405, 502...).
        if (ex instanceof ResponseStatusException rse) {
            return porStatus(exchange, rse);
        }
        // Caso 3: cualquier otra cosa -> 500, sin mostrar detalles internos al cliente (quedan en el log).
        log.error("Error inesperado en el gateway en {}", exchange.getRequest().getPath(), ex);
        return respuestas.escribir(exchange, HttpStatus.INTERNAL_SERVER_ERROR, CodigosError.ERROR_INTERNO,
                "Ocurrió un error inesperado");
    }

    /** Traduce un código HTTP a la respuesta de error de ClimaFit. */
    private Mono<Void> porStatus(ServerWebExchange exchange, ResponseStatusException ex) {
        int status = ex.getStatusCode().value();
        return switch (status) {
            case 404 -> respuestas.escribir(exchange, HttpStatus.NOT_FOUND, CodigosError.NO_ENCONTRADO,
                    "El recurso no existe");
            case 405 -> respuestas.escribir(exchange, HttpStatus.METHOD_NOT_ALLOWED, CodigosError.DATOS_INVALIDOS,
                    "Método HTTP no soportado para esta ruta");
            // 502/503/504 del gateway: el microservicio no respondió a tiempo o no está.
            case 502, 503, 504 -> respuestas.escribir(exchange, HttpStatus.SERVICE_UNAVAILABLE,
                    CodigosError.SERVICIO_NO_DISPONIBLE, "El servicio " + servicioDestino(exchange)
                            + " no está disponible. Intentá de nuevo en unos minutos");
            default -> {
                HttpStatus resuelto = HttpStatus.resolve(status);
                if (resuelto != null && resuelto.is4xxClientError()) {
                    yield respuestas.escribir(exchange, resuelto, CodigosError.DATOS_INVALIDOS,
                            "El pedido no es válido");
                }
                log.error("Error inesperado en el gateway en {}", exchange.getRequest().getPath(), ex);
                yield respuestas.escribir(exchange, HttpStatus.INTERNAL_SERVER_ERROR, CodigosError.ERROR_INTERNO,
                        "Ocurrió un error inesperado");
            }
        };
    }

    /** Conexión rechazada, host desconocido, conexión cortada o timeout, en cualquier nivel de la causa. */
    private static boolean esFallaDeConexion(Throwable ex) {
        for (Throwable causa = ex; causa != null; causa = causa.getCause()) {
            if (causa instanceof IOException || causa instanceof TimeoutException) {
                return true;
            }
            if (causa.getCause() == causa) {
                break;
            }
        }
        return false;
    }

    /** Nombre del microservicio al que iba el pedido (el "id" de la ruta en application.yml). */
    private static String servicioDestino(ServerWebExchange exchange) {
        Route ruta = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        return ruta != null ? ruta.getId() : "solicitado";
    }
}
