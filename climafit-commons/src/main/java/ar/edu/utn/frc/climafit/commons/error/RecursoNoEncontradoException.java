package ar.edu.utn.frc.climafit.commons.error;

import org.springframework.http.HttpStatus;

/** 404: el recurso no existe (o el usuario no debe saber que existe, como en RN08). */
public class RecursoNoEncontradoException extends NegocioException {

    public RecursoNoEncontradoException(String mensaje) {
        super("RECURSO_NO_ENCONTRADO", mensaje, HttpStatus.NOT_FOUND);
    }
}
