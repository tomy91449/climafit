package ar.edu.utn.frc.climafit.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Rutas para el Swagger UI unificado del gateway (propuesta, sección 10).
 * <p>
 * Explicado simple: cada microservicio describe su propia API en un documento (OpenAPI, en
 * {@code /v3/api-docs}). Esta clase hace que el gateway junte los 4 documentos en una sola página:
 * {@code http://localhost:8080/swagger-ui.html}, donde arriba a la derecha se elige el microservicio.
 * <p>
 * {@code GET /docs/<servicio>} devuelve el {@code /v3/api-docs} de ese microservicio, cambiando su lista
 * de "servers" por {@code /api}. Así, "Try it out" en el Swagger del gateway llama a
 * {@code http://localhost:8080/api/...}: el pedido pasa por el gateway y por su Security Filter, igual
 * que uno de Postman. Sin ese cambio, cada servicio anunciaría su propia dirección interna de Docker.
 */
@Configuration
public class DocumentacionGatewayConfig {

    /** Prefijo público de la API en el gateway. */
    static final String PREFIJO_API = "/api";

    /**
     * Define 4 rutas extra (además de las de application.yml), una por microservicio:
     * /docs/actividades, /docs/clases, /docs/clima y /docs/notificaciones.
     * Las direcciones de cada microservicio salen de variables de entorno (las pone docker-compose.yml);
     * si no están, se usan las de localhost para correrlo desde el IDE.
     */
    @Bean
    public RouteLocator rutasDocumentacion(RouteLocatorBuilder rutas, ObjectMapper objectMapper,
                                           @Value("${MS_ACTIVIDADES_URL:http://localhost:8081}") String actividades,
                                           @Value("${MS_CLASES_URL:http://localhost:8082}") String clases,
                                           @Value("${MS_CLIMA_URL:http://localhost:8083}") String clima,
                                           @Value("${MS_NOTIFICACIONES_URL:http://localhost:8084}") String notificaciones) {
        return rutas.routes()
                .route("docs-actividades", r -> r.path("/docs/actividades")
                        .filters(f -> documentacion(f, objectMapper)).uri(actividades))
                .route("docs-clases", r -> r.path("/docs/clases")
                        .filters(f -> documentacion(f, objectMapper)).uri(clases))
                .route("docs-clima", r -> r.path("/docs/clima")
                        .filters(f -> documentacion(f, objectMapper)).uri(clima))
                .route("docs-notificaciones", r -> r.path("/docs/notificaciones")
                        .filters(f -> documentacion(f, objectMapper)).uri(notificaciones))
                .build();
    }

    /** Lo que se le hace a cada pedido de documentación: se pide /v3/api-docs y se corrige la respuesta. */
    private static GatewayFilterSpec documentacion(GatewayFilterSpec filtros, ObjectMapper objectMapper) {
        return filtros
                .setPath("/v3/api-docs")
                .modifyResponseBody(String.class, String.class,
                        (exchange, cuerpo) -> Mono.justOrEmpty(conServidorDelGateway(cuerpo, objectMapper)));
    }

    /**
     * Reemplaza la lista "servers" del documento OpenAPI por una sola entrada, {@code /api}.
     * Si el cuerpo no es un JSON de objeto (por ejemplo, un error), lo devuelve sin cambios.
     */
    static String conServidorDelGateway(String cuerpo, ObjectMapper objectMapper) {
        if (cuerpo == null) {
            return null;
        }
        try {
            JsonNode raiz = objectMapper.readTree(cuerpo);
            if (!(raiz instanceof ObjectNode documento)) {
                return cuerpo;
            }
            documento.putArray("servers").addObject()
                    .put("url", PREFIJO_API)
                    .put("description", "API Gateway");
            return objectMapper.writeValueAsString(documento);
        } catch (Exception e) {
            return cuerpo;
        }
    }
}
