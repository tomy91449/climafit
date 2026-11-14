package ar.edu.utn.frc.climafit.clases.aviso;

import ar.edu.utn.frc.climafit.commons.error.ServicioNoDisponibleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Implementación real: POST /internal/notificaciones en ms-notificaciones (responde 202).
 *
 * TODO (Persona 2): igual que en ActividadesClientReal, falta enchufar la fábrica de RestClient de
 * climafit-commons (token de servicio, timeouts). Sin token, ms-notificaciones responderá 401 porque
 * /internal/** exige ROLE_SERVICIO.
 */
@Component
@Profile("!stub")
public class NotificacionesClientReal implements NotificacionesClient {

    private final RestClient rest;

    public NotificacionesClientReal(RestClient.Builder builder,
                                    @Value("${climafit.clientes.notificaciones-url}") String baseUrl) {
        this.rest = builder.baseUrl(baseUrl).build();
    }

    @Override
    public void enviar(EventoNotificacion evento) {
        try {
            rest.post().uri("/internal/notificaciones")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(evento)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw new ServicioNoDisponibleException(
                    "ms-notificaciones respondió " + e.getStatusCode().value(), e);
        } catch (ResourceAccessException e) {
            throw new ServicioNoDisponibleException("No se pudo conectar con ms-notificaciones", e);
        }
    }
}
