package ar.edu.utn.frc.climafit.clases.client.clima;

import ar.edu.utn.frc.climafit.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.climafit.commons.trazabilidad.CorrelationIdFilter;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Cliente real de ms-clima: POST /internal/evaluaciones.
 *
 * <p>Obtiene un token OAuth2 client-credentials para el rol SERVICIO y propaga el correlation id
 * de la solicitud. No reintenta: es una operación POST y la regla común reintenta solo lecturas.
 */
@Component
@Profile("!stub")
public class ClimaClientHttp implements ClimaClient {

    record SolicitudEvaluacion(double latitud, double longitud, CriteriosClimaDto criterios,
                               List<VentanaDto> ventanas) {
    }

    record RespuestaEvaluaciones(List<EvaluacionClimaDto> evaluaciones) {
    }

    private final RestClient http;

    public ClimaClientHttp(RestClient.Builder builder,
                           OAuth2AuthorizedClientManager authorizedClientManager,
                           @Value("${climafit.clients.clima-url}") String url) {
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        fabrica.setReadTimeout(Duration.ofSeconds(5));
        this.http = builder.baseUrl(url)
                .requestFactory(fabrica)
                .requestInterceptor((request, body, execution) -> {
                    try {
                        OAuth2AuthorizedClient cliente = authorizedClientManager.authorize(
                                OAuth2AuthorizeRequest.withClientRegistrationId("climafit-servicios")
                                        .principal("ms-clases")
                                        .build());
                        if (cliente == null) {
                            throw new ServicioNoDisponibleException(
                                    "Keycloak no devolvió un token para el cliente de servicios");
                        }
                        request.getHeaders().setBearerAuth(cliente.getAccessToken().getTokenValue());
                    } catch (OAuth2AuthorizationException e) {
                        throw new ServicioNoDisponibleException(
                                "No se pudo obtener el token de servicio de Keycloak", e);
                    }
                    String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
                    if (correlationId != null && !correlationId.isBlank()) {
                        request.getHeaders().set(CorrelationIdFilter.HEADER, correlationId);
                    }
                    return execution.execute(request, body);
                })
                .build();
    }

    @Override
    public List<EvaluacionClimaDto> evaluar(double latitud, double longitud, CriteriosClimaDto criterios,
                                            List<VentanaDto> ventanas) {
        try {
            RespuestaEvaluaciones r = http.post()
                    .uri("/internal/evaluaciones")
                    .body(new SolicitudEvaluacion(latitud, longitud, criterios, ventanas))
                    .retrieve()
                    .body(RespuestaEvaluaciones.class);
            if (r == null || r.evaluaciones() == null || r.evaluaciones().size() != ventanas.size()) {
                throw new ServicioNoDisponibleException("ms-clima devolvió una respuesta incompleta");
            }
            return r.evaluaciones();
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("No se pudo consultar ms-clima", e);
        }
    }
}
