package ar.edu.utn.frc.climafit.commons.error;

import org.springframework.http.HttpStatus;

/** 404 con código NO_ENCONTRADO (decisión de Fase 0). */
public class RecursoNoEncontradoException extends NegocioException {

    public RecursoNoEncontradoException(String mensaje) {
        super("NO_ENCONTRADO", HttpStatus.NOT_FOUND, mensaje);
    }
}
