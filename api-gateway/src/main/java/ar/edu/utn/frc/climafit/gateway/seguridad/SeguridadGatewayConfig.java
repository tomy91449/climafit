package ar.edu.utn.frc.climafit.gateway.seguridad;

import ar.edu.utn.frc.climafit.gateway.error.CodigosError;
import ar.edu.utn.frc.climafit.gateway.error.RespuestasError;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.firewall.ServerExchangeRejectedHandler;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;

import static ar.edu.utn.frc.climafit.gateway.seguridad.Roles.ADMIN;
import static ar.edu.utn.frc.climafit.gateway.seguridad.Roles.ALUMNO;
import static ar.edu.utn.frc.climafit.gateway.seguridad.Roles.PROFESOR;
import static ar.edu.utn.frc.climafit.gateway.seguridad.Roles.SERVICIO;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

/**
 * Security Filter del gateway: primer nivel de la seguridad (propuesta, secciones 06 y 10.1).
 * <ol>
 *   <li>Valida el JWT (firma con las claves de Keycloak, vencimiento y emisor): si falta o no es válido, 401.</li>
 *   <li>Aplica las reglas "gruesas" por ruta y método según el ROL (tabla de la sección 07): si no alcanza, 403.</li>
 * </ol>
 * Lo que el gateway no puede decidir (¿este profesor es el dueño de ESTA clase?) lo resuelve cada
 * microservicio, que además vuelve a validar el token con climafit-commons (segundo nivel, defensa
 * en profundidad). Las rutas /internal/** las corta antes {@link BloqueoRutasInternasFilter}.
 * <p>
 * Las reglas se evalúan en orden y gana la PRIMERA que coincide: las más específicas van arriba.
 * Lo que no está listado solo exige un token válido; el rol lo controla el microservicio.
 */
@Configuration
@EnableWebFluxSecurity
public class SeguridadGatewayConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, RespuestasError respuestas) {
        http
                // API sin estado: sin sesión, sin cookies, sin formulario de login ni CSRF.
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .requestCache(cache -> cache.requestCache(NoOpServerRequestCache.getInstance()))

                .authorizeExchange(rutas -> rutas
                        .pathMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        // Swagger UI unificado y la documentación OpenAPI de cada servicio: son públicos,
                        // como en los microservicios. Para EJECUTAR un endpoint igual hace falta el token.
                        .pathMatchers(GET, "/swagger-ui.html", "/swagger-ui/**", "/webjars/**",
                                "/v3/api-docs/**", "/docs/**").permitAll()

                        // ---- ms-actividades: catálogo ----
                        // Actividades y espacios: todos leen, solo ADMIN registra, modifica o da de baja.
                        .pathMatchers(GET, "/api/actividades/**", "/api/espacios/**").authenticated()
                        .pathMatchers("/api/actividades/**", "/api/espacios/**").hasRole(ADMIN)
                        // Ejercicios: compatibilidad (RN07) la usa el profesor al armar la rutina.
                        .pathMatchers(POST, "/api/ejercicios/compatibilidad").hasAnyRole(PROFESOR, SERVICIO)
                        .pathMatchers(GET, "/api/ejercicios/**").authenticated()
                        // ADMIN (base) y PROFESOR (personalizados). La propiedad (RN08) la valida ms-actividades.
                        .pathMatchers("/api/ejercicios/**").hasAnyRole(ADMIN, PROFESOR)

                        // ---- ms-clases: clases, evaluaciones e inscripciones ----
                        .pathMatchers(POST, "/api/clases/evaluaciones").hasRole(ADMIN)
                        .pathMatchers(POST, "/api/clases/*/inscripciones").hasRole(ALUMNO)
                        .pathMatchers(GET, "/api/clases/*/inscripciones").hasAnyRole(PROFESOR, ADMIN)
                        .pathMatchers(PATCH, "/api/clases/*/reprogramacion", "/api/clases/*/decision-riesgo")
                        .hasRole(PROFESOR)
                        .pathMatchers(PATCH, "/api/clases/*/cancelacion").hasAnyRole(PROFESOR, ADMIN)
                        .pathMatchers("/api/clases/*/evaluaciones", "/api/clases/*/historial")
                        .hasAnyRole(PROFESOR, ADMIN)
                        .pathMatchers(POST, "/api/clases").hasRole(PROFESOR)
                        .pathMatchers(PUT, "/api/clases/*").hasAnyRole(PROFESOR, ADMIN)
                        // Inscripciones propias (consultar y cancelar): solo el alumno.
                        .pathMatchers("/api/inscripciones/**").hasRole(ALUMNO)

                        // ---- ms-clima ----
                        // Simulación (perfil demo): solo ADMIN. Pronóstico: profesores y admin.
                        .pathMatchers("/api/clima/modo", "/api/clima/modo/**", "/api/clima/escenarios/**")
                        .hasRole(ADMIN)
                        .pathMatchers("/api/clima/**").hasAnyRole(PROFESOR, ADMIN)

                        // ---- ms-notificaciones ----
                        // El contacto propio lo gestiona cualquier usuario autenticado (cae en anyExchange).
                        .pathMatchers("/api/notificaciones/**").hasAnyRole(PROFESOR, ADMIN)

                        .anyExchange().authenticated())

                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint((exchange, ex) -> respuestas.escribir(exchange,
                                HttpStatus.UNAUTHORIZED, CodigosError.NO_AUTENTICADO, "Se requiere un token válido"))
                        .accessDeniedHandler((exchange, ex) -> respuestas.escribir(exchange,
                                HttpStatus.FORBIDDEN, CodigosError.ACCESO_DENEGADO,
                                "No tenés permisos para realizar esta operación")))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint((exchange, ex) -> respuestas.escribir(exchange,
                                HttpStatus.UNAUTHORIZED, CodigosError.NO_AUTENTICADO, "Se requiere un token válido"))
                        .accessDeniedHandler((exchange, ex) -> respuestas.escribir(exchange,
                                HttpStatus.FORBIDDEN, CodigosError.ACCESO_DENEGADO,
                                "No tenés permisos para realizar esta operación")));
        return http.build();
    }

    /**
     * El firewall de Spring Security rechaza rutas peligrosas (barras codificadas %2F, caracteres
     * de control, etc.). Con este handler el 400 sale en el formato común en vez de vacío.
     */
    @Bean
    public ServerExchangeRejectedHandler serverExchangeRejectedHandler(RespuestasError respuestas) {
        return (exchange, ex) -> respuestas.escribir(exchange, HttpStatus.BAD_REQUEST, CodigosError.DATOS_INVALIDOS,
                "La ruta contiene caracteres no permitidos");
    }

    private ReactiveJwtAuthenticationConverterAdapter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRolesConverter());
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }
}
