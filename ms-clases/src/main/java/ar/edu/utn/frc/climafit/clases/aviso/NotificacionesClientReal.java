package ar.edu.utn.frc.climafit.clases.aviso;

import ar.edu.utn.frc.climafit.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.climafit.commons.http.ClienteServicioFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Implementación real: POST /internal/notificaciones en ms-notificaciones (responde 202).
 *
 * El RestClient lo arma ClienteServicioFactory de climafit-commons: token de servicio (client credentials),
 * timeouts de 2 s / 5 s, reintentos en lecturas y propagación del correlation id.
 */
@Component
@Profile("!stub")
public class NotificacionesClientReal implements NotificacionesClient {

    private final RestClient rest;

    public NotificacionesClientReal(ClienteServicioFactory fabrica,
                                    @Value("${climafit.clientes.notificaciones-url}") String baseUrl) {
        this.rest = fabrica.crear(baseUrl);
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
        } catch (RestClientException e) {
            // Sin conexión, timeout (también a mitad de la respuesta) o respuesta ilegible.
            throw new ServicioNoDisponibleException("ms-notificaciones no respondió a tiempo o no se pudo conectar", e);
        }
    }
}
