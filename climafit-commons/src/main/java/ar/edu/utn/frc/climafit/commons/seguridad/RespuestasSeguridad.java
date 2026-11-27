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
 *
 * <p>Para principiantes: cuando Spring Security rechaza un pedido, por defecto responde con un
 * cuerpo vacío o con su propio formato. Esta clase hace que el rechazo salga con el mismo JSON que
 * el resto de los errores de ClimaFit ({@link ErrorResponse}). Es el equivalente, en los
 * microservicios, de {@code RespuestasError} del API Gateway.
 *
 * <p>La registra ClimafitSeguridadAutoConfiguration en la cadena de seguridad.
 */
public class RespuestasSeguridad {

    // Jackson: convierte objetos Java a JSON.
    private final ObjectMapper objectMapper;

    public RespuestasSeguridad(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 401 NO AUTENTICADO: no vino token, o vino uno inválido o vencido.
     * Spring Security llama a este "punto de entrada" cuando el usuario no se identificó.
     */
    public AuthenticationEntryPoint noAutenticado() {
        return (request, response, excepcion) -> {
            // Estándar HTTP: avisa al cliente que tiene que mandar un token de tipo Bearer.
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            escribir(request, response, HttpStatus.UNAUTHORIZED, CodigosError.NO_AUTENTICADO,
                    "Se requiere un token válido");
        };
    }

    /**
     * 403 ACCESO DENEGADO: el token es válido pero el usuario no tiene el rol necesario
     * (por ejemplo, un alumno que intenta crear una clase).
     */
    public AccessDeniedHandler accesoDenegado() {
        return (request, response, excepcion) -> escribir(request, response, HttpStatus.FORBIDDEN,
                CodigosError.ACCESO_DENEGADO, "No tenés permisos para realizar esta operación");
    }

    /** Arma el JSON de error y lo escribe en la respuesta. */
    private void escribir(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
                          String codigo, String mensaje) throws IOException {
        // El cuerpo común (status, código, mensaje, path, correlation id y fecha).
        ErrorResponse cuerpo = ErrorResponseFactory.crear(status.value(), codigo, mensaje, request);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name()); // para que salgan bien las tildes
        objectMapper.writeValue(response.getOutputStream(), cuerpo);
    }
}
