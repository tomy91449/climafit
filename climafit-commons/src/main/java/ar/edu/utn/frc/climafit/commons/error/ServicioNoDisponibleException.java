package ar.edu.utn.frc.climafit.commons.error;

import org.springframework.http.HttpStatus;

/** 503 SERVICIO_NO_DISPONIBLE: otro servicio (interno o externo) no respondió a tiempo o falló. */
public class ServicioNoDisponibleException extends NegocioException {

    public ServicioNoDisponibleException(String mensaje) {
        super(HttpStatus.SERVICE_UNAVAILABLE, CodigosError.SERVICIO_NO_DISPONIBLE, mensaje);
    }

    public ServicioNoDisponibleException(String mensaje, Throwable causa) {
        this(mensaje);
        initCause(causa);
    }
}
