package ar.edu.utn.frc.climafit.commons.error;

import org.springframework.http.HttpStatus;

/** 403 ACCESO_DENEGADO: el usuario está autenticado pero no puede operar sobre ese recurso. */
public class AccesoDenegadoException extends NegocioException {

    public AccesoDenegadoException(String mensaje) {
        super(HttpStatus.FORBIDDEN, CodigosError.ACCESO_DENEGADO, mensaje);
    }
}
