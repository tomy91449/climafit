package ar.edu.utn.frc.climafit.commons.seguridad;

import ar.edu.utn.frc.climafit.commons.error.CodigosError;
import ar.edu.utn.frc.climafit.commons.error.ErrorResponse;
import ar.edu.utn.frc.climafit.commons.error.ErrorResponseFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Respuestas 401 y 403 de la cadena de seguridad, en el formato común de errores.
 * El 401 lleva la cabecera {@code WWW-Authenticate: Bearer} (propuesta, sección 10.1).
 */
public class RespuestasSeguridad {

    private final ObjectMapper objectMapper;

    public RespuestasSeguridad(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AuthenticationEntryPoint noAutenticado() {
        return (request, response, excepcion) -> {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            escribir(request, response, HttpStatus.UNAUTHORIZED, CodigosError.NO_AUTENTICADO,
                    "Se requiere un token válido");
        };
    }

    public AccessDeniedHandler accesoDenegado() {
        return (request, response, excepcion) -> escribir(request, response, HttpStatus.FORBIDDEN,
                CodigosError.ACCESO_DENEGADO, "No tenés permisos para realizar esta operación");
    }

    private void escribir(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
                          String codigo, String mensaje) throws IOException {
        ErrorResponse cuerpo = ErrorResponseFactory.crear(status.value(), codigo, mensaje, request);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), cuerpo);
    }
}
