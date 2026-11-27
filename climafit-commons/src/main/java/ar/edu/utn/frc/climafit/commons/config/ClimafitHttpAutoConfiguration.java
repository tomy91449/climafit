package ar.edu.utn.frc.climafit.commons.config;

import ar.edu.utn.frc.climafit.commons.http.ClienteServicioFactory;
import ar.edu.utn.frc.climafit.commons.http.TokenServicio;
import ar.edu.utn.frc.climafit.commons.http.TokenServicioOAuth2;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP entre servicios. El token de servicio se activa solo si el servicio configura el
 * registro OAuth2 {@code climafit-servicios} en su application.yml:
 *
 * <pre>
 * spring.security.oauth2.client:
 *   registration.climafit-servicios:
 *     client-id: climafit-servicios
 *     client-secret: ${CLIENTE_SERVICIOS_SECRET}
 *     authorization-grant-type: client_credentials
 *   provider.climafit-servicios:
 *     token-uri: http://keycloak:8080/realms/climafit/protocol/openid-connect/token
 * </pre>
 *
 * <p>Para principiantes: esta configuración arma, en cadena, las 3 piezas para que un microservicio
 * llame a otro:
 * <ol>
 *   <li>el "manager" OAuth2, que habla con Keycloak (solo si el yml tiene el registro de arriba);</li>
 *   <li>el {@link TokenServicio}, que usa ese manager para entregar el token;</li>
 *   <li>la {@link ClienteServicioFactory}, que arma los clientes HTTP con el token, timeouts y reintentos.</li>
 * </ol>
 * Hoy el único que configura el registro es ms-clases (es el que llama a los demás).
 *
 * <p>No tiene la condición SERVLET, por eso el API Gateway la excluye a mano en su application.yml
 * ({@code spring.autoconfigure.exclude}): el gateway reenvía pedidos, no usa RestClient.
 */
// afterName: se carga DESPUÉS de las configuraciones de Spring que crean los registros OAuth2 y el
// RestClient.Builder, para poder usarlos.
@AutoConfiguration(afterName = {
        "org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration",
        "org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration",
        "org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration"
})
@EnableConfigurationProperties(ClimafitProperties.class)
public class ClimafitHttpAutoConfiguration {

    /**
     * Pieza 1: el manager que pide tokens a Keycloak con client credentials y los guarda en memoria
     * hasta que vencen. Solo se crea si existen registros OAuth2 en el yml (ClientRegistrationRepository).
     */
    @Bean
    @ConditionalOnBean(ClientRegistrationRepository.class)
    @ConditionalOnMissingBean(OAuth2AuthorizedClientManager.class)
    public OAuth2AuthorizedClientManager climafitAuthorizedClientManager(
            ClientRegistrationRepository registros,
            ObjectProvider<OAuth2AuthorizedClientService> servicioExistente) {
        // Dónde se guardan los tokens obtenidos: el que ya exista o uno nuevo en memoria.
        OAuth2AuthorizedClientService servicio =
                servicioExistente.getIfAvailable(() -> new InMemoryOAuth2AuthorizedClientService(registros));
        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(registros, servicio);
        // Solo se habilita client credentials (programa a programa, sin usuario humano).
        manager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build());
        return manager;
    }

    /** Pieza 2: el que entrega el token de servicio. Solo si existe la pieza 1. */
    @Bean
    @ConditionalOnBean(OAuth2AuthorizedClientManager.class)
    @ConditionalOnMissingBean(TokenServicio.class)
    public TokenServicio tokenServicio(OAuth2AuthorizedClientManager manager, ClimafitProperties propiedades) {
        return new TokenServicioOAuth2(manager, propiedades.getHttp().getRegistroCliente());
    }

    /**
     * Pieza 3: la fábrica de clientes HTTP. Se crea siempre; si no hay TokenServicio, los clientes
     * funcionan igual pero sin token (ObjectProvider permite pedir algo que puede no existir).
     */
    @Bean
    @ConditionalOnMissingBean
    public ClienteServicioFactory clienteServicioFactory(ObjectProvider<RestClient.Builder> builder,
                                                         ClimafitProperties propiedades,
                                                         ObjectProvider<TokenServicio> tokenServicio) {
        return new ClienteServicioFactory(builder.getIfAvailable(RestClient::builder),
                propiedades.getHttp(), tokenServicio.getIfAvailable());
    }
}
