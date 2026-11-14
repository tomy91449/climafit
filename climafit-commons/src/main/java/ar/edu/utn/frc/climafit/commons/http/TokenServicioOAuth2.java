package ar.edu.utn.frc.climafit.commons.http;

import ar.edu.utn.frc.climafit.commons.error.ServicioNoDisponibleException;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;

/**
 * Obtiene el token de servicio con el grant client credentials contra Keycloak.
 * El {@link OAuth2AuthorizedClientManager} lo guarda y lo reutiliza hasta que vence;
 * recién entonces pide uno nuevo (propuesta, sección 09: "con caché hasta que vence").
 */
public class TokenServicioOAuth2 implements TokenServicio {

    /** Nombre con el que se guarda el token en la caché del manager (no es un usuario real). */
    private static final String PRINCIPAL = "climafit-servicio";

    private final OAuth2AuthorizedClientManager manager;
    private final String registroCliente;

    public TokenServicioOAuth2(OAuth2AuthorizedClientManager manager, String registroCliente) {
        this.manager = manager;
        this.registroCliente = registroCliente;
    }

    @Override
    public String obtener() {
        OAuth2AuthorizeRequest pedido = OAuth2AuthorizeRequest
                .withClientRegistrationId(registroCliente)
                .principal(PRINCIPAL)
                .build();
        OAuth2AuthorizedClient cliente;
        try {
            cliente = manager.authorize(pedido);
        } catch (RuntimeException ex) {
            throw new ServicioNoDisponibleException("No se pudo obtener el token de servicio de Keycloak", ex);
        }
        if (cliente == null || cliente.getAccessToken() == null) {
            throw new ServicioNoDisponibleException("Keycloak no entregó el token de servicio");
        }
        return cliente.getAccessToken().getTokenValue();
    }
}
