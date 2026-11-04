package ar.edu.utn.frc.climafit.commons;

import ar.edu.utn.frc.climafit.commons.error.EscritorErrores;
import ar.edu.utn.frc.climafit.commons.error.ManejadorGlobalErrores;
import ar.edu.utn.frc.climafit.commons.seguridad.KeycloakRolesConverter;
import ar.edu.utn.frc.climafit.commons.seguridad.UsuarioActual;
import ar.edu.utn.frc.climafit.commons.trazabilidad.CorrelationIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Autoconfiguración de la librería: con solo agregar la dependencia en el pom.xml, el microservicio
 * recibe todos estos beans (Spring Boot la descubre por el archivo
 * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports).
 * <p>
 * Se ejecuta ANTES que la autoconfiguración de seguridad de Spring Boot, para que la cadena de
 * seguridad que definimos acá reemplace a la que Spring crearía por defecto.
 */
@AutoConfiguration(
        before = {SecurityAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class},
        beforeName = "org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableWebSecurity
@EnableMethodSecurity // habilita @PreAuthorize en los controladores
public class ClimafitCommonsAutoConfiguration {

    // ---------- Errores ----------

    @Bean
    public EscritorErrores escritorErrores(ObjectMapper objectMapper) {
        return new EscritorErrores(objectMapper);
    }

    @Bean
    public ManejadorGlobalErrores manejadorGlobalErrores(EscritorErrores escritorErrores) {
        return new ManejadorGlobalErrores(escritorErrores);
    }

    // ---------- Trazabilidad ----------

    /** Corre primero que todo (incluso antes que la seguridad) para que un 401 también lleve correlation id. */
    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter() {
        FilterRegistrationBean<CorrelationIdFilter> registro = new FilterRegistrationBean<>(new CorrelationIdFilter());
        registro.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registro;
    }

    // ---------- Seguridad ----------

    @Bean
    public UsuarioActual usuarioActual() {
        return new UsuarioActual();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRolesConverter());
        return converter;
    }

    /** 401: falta el token, está vencido o la firma no es válida. */
    @Bean
    public AuthenticationEntryPoint climafitAuthenticationEntryPoint(EscritorErrores errores) {
        return (request, response, ex) -> {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            errores.escribir(response, HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO",
                    "Se requiere un token válido", request);
        };
    }

    /** 403: el token es válido pero el rol no alcanza para la ruta. */
    @Bean
    public AccessDeniedHandler climafitAccessDeniedHandler(EscritorErrores errores) {
        return (request, response, ex) -> errores.escribir(response, HttpStatus.FORBIDDEN, "ACCESO_DENEGADO",
                "No tiene permisos para realizar esta operación", request);
    }

    /**
     * Configuración base de "resource server": cada microservicio vuelve a validar el JWT aunque
     * el gateway ya lo haya hecho (defensa en profundidad: la red interna no se considera confiable
     * por sí sola). Las reglas finas van en cada controlador con @PreAuthorize.
     * <p>
     * Si un servicio necesita reglas distintas, define su propio SecurityFilterChain y este no se crea.
     */
    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain climafitSecurityFilterChain(HttpSecurity http,
                                                           JwtAuthenticationConverter jwtAuthenticationConverter,
                                                           AuthenticationEntryPoint entryPoint,
                                                           AccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                // API sin sesiones ni cookies: cada solicitud trae su token, así que CSRF no aplica.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/v3/api-docs/**", "/swagger-ui/**",
                                "/swagger-ui.html", "/error").permitAll()
                        // Trusted zone: solo otros microservicios, con el token de la service account.
                        .requestMatchers("/internal/**").hasRole(UsuarioActual.SERVICIO)
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }
}
