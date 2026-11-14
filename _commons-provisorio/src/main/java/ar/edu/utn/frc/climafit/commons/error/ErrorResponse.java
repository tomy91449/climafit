package ar.edu.utn.frc.climafit.commons.error;

import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** Formato de error único (Fase 0, punto 5). */
public record ErrorResponse(int status, String codigo, String mensaje, String ruta,
                            String correlationId, LocalDateTime fecha) {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Cordoba");

    public static ErrorResponse de(int status, String codigo, String mensaje, HttpServletRequest request) {
        return new ErrorResponse(status, codigo, mensaje, request.getRequestURI(),
                correlationId(request), LocalDateTime.now(ZONA).truncatedTo(ChronoUnit.SECONDS));
    }

    public static String correlationId(HttpServletRequest request) {
        String cabecera = request.getHeader("X-Correlation-Id");
        return (cabecera != null && !cabecera.isBlank()) ? cabecera : UUID.randomUUID().toString().substring(0, 6);
    }
}
