package ar.edu.utn.frc.climafit.commons.http;

import ar.edu.utn.frc.climafit.commons.error.ServicioNoDisponibleException;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;

/**
 * Obtiene el token de servicio con el grant client credentials contra Keycloak.
 * El {@link OAuth2AuthorizedClientManager} lo guarda y lo reutiliza hasta que vence;
 * recién entonces pide uno nuevo (propuesta, sección 09: "con caché hasta que vence").
 *
 * <p>Para principiantes: "client credentials" es la forma de pedir un token cuando no hay una
 * persona, sino un programa. ms-clases se presenta ante Keycloak con un usuario y una contraseña
 * propios del sistema (client-id "climafit-servicios" y su secreto del .env) y Keycloak le devuelve
 * un JWT con el rol SERVICIO. Con ese token puede entrar a las rutas /internal/** de los otros servicios.
 *
 * <p>Los datos de conexión (client-id, secreto y dirección de Keycloak) están en el application.yml
 * de ms-clases, en {@code spring.security.oauth2.client}.
 */
public class TokenServicioOAuth2 implements TokenServicio {

    /** Nombre con el que se guarda el token en la caché del manager (no es un usuario real). */
    private static final String PRINCIPAL = "climafit-servicio";

    // El "manager" es la pieza de Spring Security que habla con Keycloak y guarda el token.
    private final OAuth2AuthorizedClientManager manager;
    // Nombre del registro del application.yml que se usa ("climafit-servicios").
    private final String registroCliente;

    public TokenServicioOAuth2(OAuth2AuthorizedClientManager manager, String registroCliente) {
        this.manager = manager;
        this.registroCliente = registroCliente;
    }

    @Override
    public String obtener() {
        // 1. Armar el pedido: "quiero el token del registro climafit-servicios".
        OAuth2AuthorizeRequest pedido = OAuth2AuthorizeRequest
                .withClientRegistrationId(registroCliente)
                .principal(PRINCIPAL)
                .build();
        OAuth2AuthorizedClient cliente;
        try {
            // 2. Si ya hay un token guardado y vigente, lo devuelve; si no, se lo pide a Keycloak.
            cliente = manager.authorize(pedido);
        } catch (RuntimeException ex) {
            // Keycloak caído o secreto incorrecto: se informa como 503.
            throw new ServicioNoDisponibleException("No se pudo obtener el token de servicio de Keycloak", ex);
        }
        if (cliente == null || cliente.getAccessToken() == null) {
            throw new ServicioNoDisponibleException("Keycloak no entregó el token de servicio");
        }
        // 3. El texto del JWT, que después va en la cabecera "Authorization: Bearer <token>".
        return cliente.getAccessToken().getTokenValue();
    }
}
