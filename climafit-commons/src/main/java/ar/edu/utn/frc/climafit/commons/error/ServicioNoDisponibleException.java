package ar.edu.utn.frc.climafit.commons.error;

import org.springframework.http.HttpStatus;

/** 503: otro microservicio o una API externa no respondió a tiempo (RN16). */
public class ServicioNoDisponibleException extends NegocioException {

    public ServicioNoDisponibleException(String mensaje) {
        super("SERVICIO_NO_DISPONIBLE", mensaje, HttpStatus.SERVICE_UNAVAILABLE);
    }

    public ServicioNoDisponibleException(String mensaje, Throwable causa) {
        this(mensaje);
        initCause(causa);
    }
}
