package ar.edu.utn.frc.climafit.commons.error;

import ar.edu.utn.frc.climafit.commons.trazabilidad.CorrelationIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * Arma el {@link ErrorResponse}. Lo usan tanto el manejador global (errores dentro de los
 * controladores) como los handlers de seguridad (401/403 que ocurren ANTES de llegar a un
 * controlador), para que todos los errores salgan con el mismo formato.
 */
public class EscritorErrores {

    private final ObjectMapper objectMapper;

    public EscritorErrores(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ErrorResponse crear(HttpStatus status, String codigo, String mensaje, HttpServletRequest request) {
        return new ErrorResponse(status.value(), codigo, mensaje, request.getRequestURI(),
                CorrelationIdFilter.actual(), OffsetDateTime.now());
    }

    /** Escribe el error directamente en la respuesta (para los filtros de seguridad). */
    public void escribir(HttpServletResponse response, HttpStatus status, String codigo, String mensaje,
                         HttpServletRequest request) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), crear(status, codigo, mensaje, request));
    }
}
