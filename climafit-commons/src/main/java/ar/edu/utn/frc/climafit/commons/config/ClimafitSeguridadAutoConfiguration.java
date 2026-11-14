package ar.edu.utn.frc.climafit.commons.config;

import ar.edu.utn.frc.climafit.commons.seguridad.KeycloakRolesConverter;
import ar.edu.utn.frc.climafit.commons.seguridad.RespuestasSeguridad;
import ar.edu.utn.frc.climafit.commons.seguridad.Roles;
import ar.edu.utn.frc.climafit.commons.seguridad.UsuarioActual;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración base de seguridad de los microservicios (propuesta, secciones 09 y 10.1):
 * <ul>
 *   <li>resource server: valida el JWT emitido por Keycloak;</li>
 *   <li>roles de realm de Keycloak como autoridades ROLE_*;</li>
 *   <li>{@code @PreAuthorize} habilitado;</li>
 *   <li>{@code /internal/**} solo para el rol SERVICIO;</li>
 *   <li>documentación OpenAPI libre y el resto autenticado;</li>
 *   <li>401 y 403 en el formato común de errores.</li>
 * </ul>
 * Un servicio puede reemplazarla declarando su propio {@link SecurityFilterChain}.
 */
@AutoConfiguration(beforeName = {
        "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration",
        "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration",
        "org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration",
        "org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration"
})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(ClimafitProperties.class)
@EnableWebSecurity
@EnableMethodSecurity
public class ClimafitSeguridadAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public KeycloakRolesConverter keycloakRolesConverter() {
        return new KeycloakRolesConverter();
    }

    @Bean
    @ConditionalOnMissingBean
    public UsuarioActual usuarioActual() {
        return new UsuarioActual();
    }

    @Bean
    @ConditionalOnMissingBean
    public RespuestasSeguridad respuestasSeguridad(ObjectMapper objectMapper) {
        return new RespuestasSeguridad(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain climafitSecurityFilterChain(HttpSecurity http,
                                                           ClimafitProperties propiedades,
                                                           KeycloakRolesConverter rolesConverter,
                                                           RespuestasSeguridad respuestas) throws Exception {
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(rolesConverter);

        String[] rutasPublicas = propiedades.getSeguridad().getRutasPublicas().toArray(String[]::new);

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reglas -> reglas
                        .requestMatchers(rutasPublicas).permitAll()
                        .requestMatchers("/internal/**").hasRole(Roles.SERVICIO)
                        .anyRequest().authenticated())
                .oauth2ResourceServer(recurso -> recurso
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
                        .authenticationEntryPoint(respuestas.noAutenticado())
                        .accessDeniedHandler(respuestas.accesoDenegado()))
                .exceptionHandling(excepciones -> excepciones
                        .authenticationEntryPoint(respuestas.noAutenticado())
                        .accessDeniedHandler(respuestas.accesoDenegado()));
        return http.build();
    }
}
