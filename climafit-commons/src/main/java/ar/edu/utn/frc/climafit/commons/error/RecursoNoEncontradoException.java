package ar.edu.utn.frc.climafit.commons.error;

import org.springframework.http.HttpStatus;

/** 404 NO_ENCONTRADO. */
public class RecursoNoEncontradoException extends NegocioException {

    public RecursoNoEncontradoException(String mensaje) {
        super(HttpStatus.NOT_FOUND, CodigosError.NO_ENCONTRADO, mensaje);
    }
}
