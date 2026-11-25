package ar.edu.utn.frc.climafit.gateway.seguridad;

import ar.edu.utn.frc.climafit.commons.error.CodigosError;
import ar.edu.utn.frc.climafit.gateway.error.RespuestasError;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Locale;

/**
 * Filtro que esconde las rutas internas. Es el SEGUNDO control por el que pasa cada pedido
 * (después de {@code CorrelationIdFilter} y antes de la seguridad).
 * <p>
 * Explicado simple: los microservicios tienen direcciones {@code /internal/...} que solo usan entre ellos
 * (por ejemplo, ms-clases le pide a ms-notificaciones que mande un aviso). Esas direcciones no tienen que
 * poder usarse desde Postman ni desde internet. Este filtro las hace "invisibles": responde 404, como si
 * no existieran.
 * <p>
 * Trusted zone (propuesta, secciones 06 y 10.1): las rutas {@code /internal/**} solo se llaman
 * entre microservicios, dentro de la red de Docker y con token de servicio. Desde afuera no existen.
 * <p>
 * Que el gateway no tenga rutas hacia /internal no alcanza: {@code /api/clima/../internal/evaluaciones}
 * coincide con la ruta {@code /api/clima/**} y, al sacarle el prefijo, el microservicio lo normaliza
 * a {@code /internal/evaluaciones}. Por eso este filtro mira cada segmento ya decodificado:
 * <ul>
 *   <li>si alguno es {@code internal} -> 404 NO_ENCONTRADO (no se revela que existe);</li>
 *   <li>si alguno es {@code .} o {@code ..} -> 400 DATOS_INVALIDOS (no se aceptan rutas relativas).</li>
 * </ul>
 * Corre antes que el Security Filter: la respuesta es la misma con token o sin token.
 * <p>
 * {@code @Component} = Spring lo crea solo. {@code @Order} = en qué posición corre entre los filtros
 * (cuanto más chico el número, antes corre).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class BloqueoRutasInternasFilter implements WebFilter {

    private static final String SEGMENTO_INTERNO = "internal";

    private final RespuestasError respuestas;

    public BloqueoRutasInternasFilter(RespuestasError respuestas) {
        this.respuestas = respuestas;
    }

    /** Se ejecuta con cada pedido que entra al gateway. */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        boolean rutaRelativa = false;
        // Recorre la dirección pedida parte por parte: /api/clima/modo -> "api", "clima", "modo".
        for (PathContainer.Element elemento : exchange.getRequest().getPath().pathWithinApplication().elements()) {
            if (!(elemento instanceof PathContainer.PathSegment segmento)) {
                continue;
            }
            // valueToMatch() ya viene decodificado (%2e%2e -> ..) y sin parámetros de matriz (;x=y).
            // Un segmento decodificado puede traer "/" o "\" (%2f, %5c): se revisa cada parte.
            for (String parte : segmento.valueToMatch().split("[/\\\\]")) {
                String valor = parte.trim().toLowerCase(Locale.ROOT);
                if (valor.equals(SEGMENTO_INTERNO)) {
                    // Gana sobre el 400: una ruta interna siempre "no existe", llegue como llegue.
                    return respuestas.escribir(exchange, HttpStatus.NOT_FOUND, CodigosError.NO_ENCONTRADO,
                            "El recurso no existe");
                }
                rutaRelativa |= valor.equals(".") || valor.equals("..");
            }
        }
        if (rutaRelativa) {
            return respuestas.escribir(exchange, HttpStatus.BAD_REQUEST, CodigosError.DATOS_INVALIDOS,
                    "La ruta no puede contener segmentos relativos");
        }
        // Todo bien: el pedido sigue al próximo control (la seguridad).
        return chain.filter(exchange);
    }
}
