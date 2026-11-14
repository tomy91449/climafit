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
 */
@AutoConfiguration(afterName = {
        "org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration",
        "org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration",
        "org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration"
})
@EnableConfigurationProperties(ClimafitProperties.class)
public class ClimafitHttpAutoConfiguration {

    @Bean
    @ConditionalOnBean(ClientRegistrationRepository.class)
    @ConditionalOnMissingBean(OAuth2AuthorizedClientManager.class)
    public OAuth2AuthorizedClientManager climafitAuthorizedClientManager(
            ClientRegistrationRepository registros,
            ObjectProvider<OAuth2AuthorizedClientService> servicioExistente) {
        OAuth2AuthorizedClientService servicio =
                servicioExistente.getIfAvailable(() -> new InMemoryOAuth2AuthorizedClientService(registros));
        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(registros, servicio);
        manager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build());
        return manager;
    }

    @Bean
    @ConditionalOnBean(OAuth2AuthorizedClientManager.class)
    @ConditionalOnMissingBean(TokenServicio.class)
    public TokenServicio tokenServicio(OAuth2AuthorizedClientManager manager, ClimafitProperties propiedades) {
        return new TokenServicioOAuth2(manager, propiedades.getHttp().getRegistroCliente());
    }

    @Bean
    @ConditionalOnMissingBean
    public ClienteServicioFactory clienteServicioFactory(ObjectProvider<RestClient.Builder> builder,
                                                         ClimafitProperties propiedades,
                                                         ObjectProvider<TokenServicio> tokenServicio) {
        return new ClienteServicioFactory(builder.getIfAvailable(RestClient::builder),
                propiedades.getHttp(), tokenServicio.getIfAvailable());
    }
}
