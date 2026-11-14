package ar.edu.utn.frc.climafit.commons.error;

import org.springframework.http.HttpStatus;

/** 503 con código SERVICIO_NO_DISPONIBLE. */
public class ServicioNoDisponibleException extends NegocioException {

    public ServicioNoDisponibleException(String mensaje) {
        super("SERVICIO_NO_DISPONIBLE", HttpStatus.SERVICE_UNAVAILABLE, mensaje);
    }

    public ServicioNoDisponibleException(String mensaje, Throwable causa) {
        super("SERVICIO_NO_DISPONIBLE", HttpStatus.SERVICE_UNAVAILABLE, mensaje, causa);
    }
}
