package ar.edu.utn.frc.climafit.commons.seguridad;

import ar.edu.utn.frc.climafit.commons.error.ErrorResponse;
import ar.edu.utn.frc.climafit.commons.error.ManejadorGlobalErrores;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;

/** Resource server base: valida el JWT, exige ROLE_SERVICIO en /internal/** y responde 401/403 en formato común. */
@AutoConfiguration(before = SecurityAutoConfiguration.class)
@EnableWebSecurity
@EnableMethodSecurity
public class ClimafitSeguridadAutoConfiguration {

    @Bean
    public UsuarioActual usuarioActual() {
        return new UsuarioActual();
    }

    @Bean
    public ManejadorGlobalErrores manejadorGlobalErrores() {
        return new ManejadorGlobalErrores();
    }

    @Bean
    public SecurityFilterChain climafitSecurityFilterChain(HttpSecurity http, ObjectMapper mapper) throws Exception {
        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(new KeycloakRolesConverter());

        http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/internal/**").hasRole("SERVICIO")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.jwtAuthenticationConverter(convertidor))
                        .authenticationEntryPoint((req, res, ex) -> {
                            res.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
                            escribir(req, res, mapper, HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO",
                                    "Token ausente, inválido o vencido");
                        })
                        .accessDeniedHandler((req, res, ex) -> escribir(req, res, mapper, HttpStatus.FORBIDDEN,
                                "ACCESO_DENEGADO", "No tiene permisos para esta operación")));
        return http.build();
    }

    private static void escribir(HttpServletRequest req, HttpServletResponse res, ObjectMapper mapper,
                                 HttpStatus status, String codigo, String mensaje) throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        mapper.writeValue(res.getWriter(), ErrorResponse.de(status.value(), codigo, mensaje, req));
    }
}
