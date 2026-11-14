package ar.edu.utn.frc.climafit.commons.error;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Formato único de error de ClimaFit (propuesta, secciones 07 y 09).
 *
 * <pre>
 * {"status": 409, "codigo": "CUPO_COMPLETO", "mensaje": "...", "ruta": "/clases/7/inscripciones",
 *  "correlationId": "8f3c2a", "fecha": "2026-11-20T18:00:00"}
 * </pre>
 *
 * {@code campos} solo aparece en los errores de validación (400 DATOS_INVALIDOS)
 * y detalla qué campo falló y por qué.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String codigo,
        String mensaje,
        String ruta,
        String correlationId,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime fecha,
        Map<String, String> campos) {

    public ErrorResponse(int status, String codigo, String mensaje, String ruta, String correlationId,
                         LocalDateTime fecha) {
        this(status, codigo, mensaje, ruta, correlationId, fecha, null);
    }
}
