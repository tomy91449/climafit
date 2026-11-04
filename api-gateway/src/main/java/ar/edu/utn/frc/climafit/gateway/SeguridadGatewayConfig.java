package ar.edu.utn.frc.climafit.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Security Filter del gateway (primer nivel de la seguridad):
 * <ol>
 *   <li>Valida el JWT (firma con las claves de Keycloak, vencimiento y emisor) -> si falla, 401.</li>
 *   <li>Aplica reglas "gruesas" por ruta y método según el ROL -> si no alcanza, 403.</li>
 * </ol>
 * Lo que el gateway NO puede decidir (¿este profesor es el dueño de ESTA clase?) lo resuelve
 * cada microservicio, que además vuelve a validar el token (segundo nivel).
 */
@Configuration
@EnableWebFluxSecurity
public class SeguridadGatewayConfig {

    private static final String ADMIN = "ADMIN";
    private static final String PROFESOR = "PROFESOR";
    private static final String ALUMNO = "ALUMNO";

    private final ObjectMapper objectMapper;

    public SeguridadGatewayConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(rutas -> rutas
                        .pathMatchers("/actuator/health/**").permitAll()

                        // Catálogo (ms-actividades): todos leen, solo ADMIN modifica actividades y espacios
                        .pathMatchers(HttpMethod.POST, "/api/actividades/**", "/api/espacios/**").hasRole(ADMIN)
                        .pathMatchers(HttpMethod.PUT, "/api/actividades/**", "/api/espacios/**").hasRole(ADMIN)
                        .pathMatchers(HttpMethod.DELETE, "/api/actividades/**", "/api/espacios/**").hasRole(ADMIN)
                        // Ejercicios: ADMIN (base) y PROFESOR (personalizados). La propiedad (RN08) la valida ms-actividades.
                        .pathMatchers(HttpMethod.POST, "/api/ejercicios/**").hasAnyRole(ADMIN, PROFESOR)
                        .pathMatchers(HttpMethod.PUT, "/api/ejercicios/**").hasAnyRole(ADMIN, PROFESOR)
                        .pathMatchers(HttpMethod.DELETE, "/api/ejercicios/**").hasAnyRole(ADMIN, PROFESOR)

                        // Clases e inscripciones (ms-clases). El orden importa: gana la primera regla que coincide.
                        .pathMatchers(HttpMethod.POST, "/api/clases/evaluaciones").hasRole(ADMIN)
                        .pathMatchers(HttpMethod.POST, "/api/clases/*/inscripciones").hasRole(ALUMNO)
                        .pathMatchers(HttpMethod.POST, "/api/clases").hasRole(PROFESOR)
                        .pathMatchers("/api/inscripciones/**").hasRole(ALUMNO)

                        // Clima: el pronóstico lo consultan profesores y admin; la simulación solo ADMIN
                        .pathMatchers("/api/clima/modo", "/api/clima/escenarios/**").hasRole(ADMIN)
                        .pathMatchers("/api/clima/**").hasAnyRole(ADMIN, PROFESOR)

                        // Notificaciones: el contacto propio lo gestiona cualquiera; el listado, profesor o admin
                        .pathMatchers("/api/notificaciones/**").hasAnyRole(ADMIN, PROFESOR)

                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter())))
                        .authenticationEntryPoint((exchange, ex) -> escribirError(exchange, HttpStatus.UNAUTHORIZED,
                                "NO_AUTENTICADO", "Se requiere un token válido"))
                        .accessDeniedHandler((exchange, ex) -> escribirError(exchange, HttpStatus.FORBIDDEN,
                                "ACCESO_DENEGADO", "No tiene permisos para realizar esta operación")));
        return http.build();
    }

    /** Mismo criterio que el KeycloakRolesConverter de climafit-commons: realm_access.roles -> ROLE_X. */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        Converter<Jwt, Collection<GrantedAuthority>> roles = jwt -> {
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> lista)) {
                return List.of();
            }
            return lista.stream().map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r)).toList();
        };
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }

    /** Mismo formato de error que los microservicios (ErrorResponse de climafit-commons). */
    private Mono<Void> escribirError(ServerWebExchange exchange, HttpStatus status, String codigo, String mensaje) {
        var response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        if (status == HttpStatus.UNAUTHORIZED) {
            response.getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("status", status.value());
        cuerpo.put("codigo", codigo);
        cuerpo.put("mensaje", mensaje);
        cuerpo.put("ruta", exchange.getRequest().getPath().value());
        cuerpo.put("correlationId", response.getHeaders().getFirst(CorrelationIdFilter.HEADER));
        cuerpo.put("fecha", OffsetDateTime.now().toString());
        try {
            DataBuffer buffer = response.bufferFactory().wrap(objectMapper.writeValueAsBytes(cuerpo));
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}
