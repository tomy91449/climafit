package ar.edu.utn.frc.climafit.commons.error;

import org.springframework.http.HttpStatus;

/** 403 con código ACCESO_DENEGADO. */
public class AccesoDenegadoException extends NegocioException {

    public AccesoDenegadoException(String mensaje) {
        super("ACCESO_DENEGADO", HttpStatus.FORBIDDEN, mensaje);
    }
}
