package ar.edu.utn.frc.climafit.gateway.error;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Mismo formato de error que los microservicios ({@code ErrorResponse} de climafit-commons):
 *
 * <pre>
 * {"status": 401, "codigo": "NO_AUTENTICADO", "mensaje": "Se requiere un token válido",
 *  "ruta": "/api/clases", "correlationId": "8f3c2a", "fecha": "2026-11-20T18:00:00"}
 * </pre>
 *
 * El gateway no depende de climafit-commons (es reactivo y la librería es servlet; propuesta,
 * sección 09), así que el formato se replica acá. Si cambia en la librería, hay que cambiarlo acá.
 */
public record ErrorResponse(
        int status,
        String codigo,
        String mensaje,
        String ruta,
        String correlationId,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime fecha) {
}
