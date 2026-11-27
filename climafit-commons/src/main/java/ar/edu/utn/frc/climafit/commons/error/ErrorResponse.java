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
 *
 * <p>Para principiantes: un {@code record} es una clase de Java que solo guarda datos (Java crea
 * solo el constructor, los getters, equals y toString). Jackson lo convierte a JSON: cada campo
 * del record pasa a ser una propiedad del JSON con el mismo nombre.
 *
 * <p>Lo usan los microservicios (ManejadorGlobalErrores, RespuestasSeguridad) y el API Gateway
 * (RespuestasError), así todos los errores del sistema se ven iguales.
 */
// NON_NULL: los campos que valen null no se escriben en el JSON (por ejemplo "campos" cuando no hay
// errores de validación).
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,              // código HTTP: 400, 401, 403, 404, 409, 503...
        String codigo,           // código de ClimaFit: DATOS_INVALIDOS, CUPO_COMPLETO... (ver CodigosError)
        String mensaje,          // texto para el usuario, en español
        String ruta,             // la URL que se pidió
        String correlationId,    // número de seguimiento para buscar el pedido en los logs
        // La fecha se escribe como texto "2026-11-20T18:00:00" en lugar de un arreglo de números.
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime fecha,     // cuándo ocurrió el error
        Map<String, String> campos) { // solo en validación: campo -> qué estuvo mal

    /** Constructor corto, para los errores que no son de validación (sin "campos"). */
    public ErrorResponse(int status, String codigo, String mensaje, String ruta, String correlationId,
                         LocalDateTime fecha) {
        this(status, codigo, mensaje, ruta, correlationId, fecha, null);
    }
}
