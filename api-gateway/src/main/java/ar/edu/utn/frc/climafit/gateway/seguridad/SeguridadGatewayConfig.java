package ar.edu.utn.frc.climafit.gateway.seguridad;

import ar.edu.utn.frc.climafit.commons.error.CodigosError;
import ar.edu.utn.frc.climafit.commons.seguridad.KeycloakRolesConverter;
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

// Los roles vienen de climafit-commons: los mismos nombres que usan los microservicios.
import static ar.edu.utn.frc.climafit.commons.seguridad.Roles.ADMIN;
import static ar.edu.utn.frc.climafit.commons.seguridad.Roles.ALUMNO;
import static ar.edu.utn.frc.climafit.commons.seguridad.Roles.PROFESOR;
import static ar.edu.utn.frc.climafit.commons.seguridad.Roles.SERVICIO;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

/**
 * Security Filter del gateway: primer nivel de la seguridad (propuesta, secciones 06 y 10.1).
 * <p>
 * Explicado simple: es el "control de acceso de la puerta". Cada pedido que llega al gateway pasa por acá
 * antes de seguir al microservicio, y se revisan dos cosas:
 * <ol>
 *   <li><b>¿Quién sos?</b> Valida el token JWT (firma con las claves de Keycloak, vencimiento y emisor).
 *       Si falta o no es válido: 401 NO_AUTENTICADO.</li>
 *   <li><b>¿Podés hacer esto?</b> Aplica las reglas "gruesas" por ruta y método según el ROL
 *       (tabla de la sección 07). Si el rol no alcanza: 403 ACCESO_DENEGADO.</li>
 * </ol>
 * Lo que el gateway no puede decidir (¿este profesor es el dueño de ESTA clase?) lo resuelve cada
 * microservicio, que además vuelve a validar el token con climafit-commons (segundo nivel, defensa
 * en profundidad). Las rutas /internal/** las corta antes {@link BloqueoRutasInternasFilter}.
 * <p>
 * Las reglas se evalúan en orden y gana la PRIMERA que coincide: las más específicas van arriba.
 * Lo que no está listado solo exige un token válido; el rol lo controla el microservicio.
 * <p>
 * {@code @Configuration} = esta clase define configuración. {@code @EnableWebFluxSecurity} = activa
 * Spring Security en su versión reactiva (WebFlux), que es la que usa el gateway.
 */
@Configuration
@EnableWebFluxSecurity
public class SeguridadGatewayConfig {

    /**
     * Arma la "cadena de seguridad": la lista de controles que se aplican a cada pedido.
     * {@code @Bean} = Spring guarda el objeto que devuelve este método y lo usa solo.
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, RespuestasError respuestas) {
        http
                // API sin estado: sin sesión, sin cookies, sin formulario de login ni CSRF.
                // Cada pedido trae su propio token; el gateway no "recuerda" a nadie entre pedidos.
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .requestCache(cache -> cache.requestCache(NoOpServerRequestCache.getInstance()))

                // ---- Reglas de acceso: qué rol puede usar cada ruta ----
                // permitAll() = sin token · authenticated() = cualquier token válido
                // hasRole(X) = solo ese rol · hasAnyRole(X, Y) = cualquiera de esos roles
                .authorizeExchange(rutas -> rutas
                        // Healthcheck de Docker: público (Docker no tiene token).
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
                        // (el * reemplaza un id, por ejemplo /api/clases/5/inscripciones)
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

                        // Todo lo demás: alcanza con un token válido.
                        .anyExchange().authenticated())

                // ---- Validación del token JWT ----
                // jwtAuthenticationConverter() le enseña a leer los roles que pone Keycloak en el token.
                // Si falla la validación: 401 (authenticationEntryPoint). Si falta el rol: 403 (accessDeniedHandler).
                // Las respuestas se escriben con RespuestasError, en el mismo JSON de error de toda la API.
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint((exchange, ex) -> respuestas.escribir(exchange,
                                HttpStatus.UNAUTHORIZED, CodigosError.NO_AUTENTICADO, "Se requiere un token válido"))
                        .accessDeniedHandler((exchange, ex) -> respuestas.escribir(exchange,
                                HttpStatus.FORBIDDEN, CodigosError.ACCESO_DENEGADO,
                                "No tenés permisos para realizar esta operación")))
                // Los mismos 401/403 para los pedidos que ni siquiera traen token.
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

    /**
     * Keycloak guarda los roles del usuario dentro del token, en "realm_access.roles" (por ejemplo ["PROFESOR"]).
     * Spring Security los espera como "ROLE_PROFESOR". KeycloakRolesConverter (de climafit-commons, el mismo
     * que usan los microservicios) hace esa traducción; acá se lo adapta a la versión reactiva del gateway.
     */
    private ReactiveJwtAuthenticationConverterAdapter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRolesConverter());
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }
}
