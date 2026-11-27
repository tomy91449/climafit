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
 *
 * <p>Para principiantes: esta es la "segunda capa" de seguridad. El API Gateway ya validó el token,
 * pero cada microservicio lo vuelve a validar con las claves públicas de Keycloak (defensa en
 * profundidad): si alguien llegara al microservicio sin pasar por el gateway, igual lo frena.
 * Además es la única capa que protege las rutas /internal/** que usan los servicios entre sí.
 *
 * <p>La dirección de Keycloak sale del application.yml de cada servicio
 * ({@code spring.security.oauth2.resourceserver.jwt.issuer-uri} y {@code jwk-set-uri}).
 */
// beforeName: se carga ANTES que la seguridad por defecto de Spring Boot, así esta es la que queda.
@AutoConfiguration(beforeName = {
        "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration",
        "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration",
        "org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration",
        "org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration"
})
// Solo en los microservicios (Spring MVC). En el API Gateway (WebFlux) no se activa: tiene SeguridadGatewayConfig.
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(ClimafitProperties.class)
@EnableWebSecurity      // prende Spring Security
@EnableMethodSecurity   // permite usar @PreAuthorize("hasRole('PROFESOR')") en los controllers
public class ClimafitSeguridadAutoConfiguration {

    /** El que lee los roles de Keycloak dentro del token. */
    @Bean
    @ConditionalOnMissingBean
    public KeycloakRolesConverter keycloakRolesConverter() {
        return new KeycloakRolesConverter();
    }

    /** "¿Quién hace este pedido?": los controllers lo reciben por el constructor. */
    @Bean
    @ConditionalOnMissingBean
    public UsuarioActual usuarioActual() {
        return new UsuarioActual();
    }

    /** Los 401 y 403 con el JSON común. */
    @Bean
    @ConditionalOnMissingBean
    public RespuestasSeguridad respuestasSeguridad(ObjectMapper objectMapper) {
        return new RespuestasSeguridad(objectMapper);
    }

    /**
     * La cadena de seguridad: las reglas que se aplican a cada pedido que llega al microservicio.
     * Solo se crea si el servicio no definió una propia.
     */
    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain climafitSecurityFilterChain(HttpSecurity http,
                                                           ClimafitProperties propiedades,
                                                           KeycloakRolesConverter rolesConverter,
                                                           RespuestasSeguridad respuestas) throws Exception {
        // Cómo convertir un JWT válido en un usuario de Spring: con los roles de Keycloak.
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(rolesConverter);

        // Swagger, /actuator/health y /error se pueden ver sin token (ver ClimafitProperties).
        String[] rutasPublicas = propiedades.getSeguridad().getRutasPublicas().toArray(String[]::new);

        http
                // CSRF protege formularios web con sesión; una API con token no lo necesita.
                .csrf(AbstractHttpConfigurer::disable)
                // STATELESS: no se guardan sesiones. Cada pedido trae su token y se valida solo.
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Reglas por ruta (se leen de arriba hacia abajo, gana la primera que coincide).
                .authorizeHttpRequests(reglas -> reglas
                        .requestMatchers(rutasPublicas).permitAll()                 // libres
                        .requestMatchers("/internal/**").hasRole(Roles.SERVICIO)    // solo el token de servicio
                        .anyRequest().authenticated())                              // el resto: token válido
                // Resource server: validar el JWT (firma con las claves de Keycloak, emisor y vencimiento).
                .oauth2ResourceServer(recurso -> recurso
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
                        .authenticationEntryPoint(respuestas.noAutenticado())    // token inválido -> 401
                        .accessDeniedHandler(respuestas.accesoDenegado()))       // sin permiso -> 403
                // Los mismos 401/403 para cualquier otro rechazo de seguridad (por ejemplo, sin token).
                .exceptionHandling(excepciones -> excepciones
                        .authenticationEntryPoint(respuestas.noAutenticado())
                        .accessDeniedHandler(respuestas.accesoDenegado()));
        return http.build();
        // Nota: los permisos finos por rol (PROFESOR, ALUMNO...) se ponen en cada controller con
        // @PreAuthorize, y los controles de dueño (RN05, RN08) en los servicios de cada microservicio.
    }
}
