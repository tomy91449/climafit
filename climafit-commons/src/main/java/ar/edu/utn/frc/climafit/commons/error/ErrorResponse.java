package ar.edu.utn.frc.climafit.commons.error;

import java.time.OffsetDateTime;

/**
 * Formato único de error de todos los microservicios.
 * Ejemplo:
 * <pre>
 * { "status": 409, "codigo": "CUPO_COMPLETO", "mensaje": "La clase no tiene cupo",
 *   "ruta": "/clases/7/inscripciones", "correlationId": "4f1c...", "fecha": "2026-11-04T18:00:00-03:00" }
 * </pre>
 * El campo {@code codigo} es el que leen los clientes (y la colección de Postman):
 * es estable, a diferencia del mensaje, que puede cambiar.
 */
public record ErrorResponse(
        int status,
        String codigo,
        String mensaje,
        String ruta,
        String correlationId,
        OffsetDateTime fecha) {
}
