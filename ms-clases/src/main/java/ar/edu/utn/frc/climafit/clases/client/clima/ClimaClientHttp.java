package ar.edu.utn.frc.climafit.clases.client.clima;

import ar.edu.utn.frc.climafit.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.climafit.commons.trazabilidad.CorrelationId;
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
import org.springframework.web.client.RestClientResponseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
        // Más que los 10 s que ms-clima le espera a Open-Meteo: si no, ms-clases cortaría primero y se
        // perdería el mensaje de ms-clima ("reintentá o comprobá tu conexión").
        fabrica.setReadTimeout(Duration.ofSeconds(15));
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
                    String correlationId = MDC.get(CorrelationId.CLAVE_MDC);
                    if (correlationId != null && !correlationId.isBlank()) {
                        request.getHeaders().set(CorrelationId.CABECERA, correlationId);
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
        } catch (RestClientResponseException e) {
            // ms-clima respondió con error (por ejemplo 503 porque Open-Meteo tardó más de 10 s):
            // se reenvía SU mensaje, que es el que le dice al usuario qué hacer.
            throw new ServicioNoDisponibleException(mensajeDeClima(e), e);
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException(
                    "El servicio de clima no respondió a tiempo. Reintentá en unos minutos.", e);
        }
    }

    /** Lee el campo "mensaje" del formato común de error; si no viene, arma uno genérico. */
    private static String mensajeDeClima(RestClientResponseException e) {
        try {
            JsonNode cuerpo = LECTOR_JSON.readTree(e.getResponseBodyAsString());
            if (cuerpo != null && cuerpo.hasNonNull("mensaje")) {
                return cuerpo.get("mensaje").asText();
            }
        } catch (Exception ignorada) {
            // el cuerpo no era JSON: se usa el mensaje genérico
        }
        return "El servicio de clima respondió con un error (" + e.getStatusCode().value()
                + "). Reintentá en unos minutos.";
    }

    private static final ObjectMapper LECTOR_JSON = new ObjectMapper();
}
