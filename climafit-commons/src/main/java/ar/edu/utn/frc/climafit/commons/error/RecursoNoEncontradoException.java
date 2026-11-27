package ar.edu.utn.frc.climafit.commons.error;

import org.springframework.http.HttpStatus;

/**
 * 404 NO_ENCONTRADO.
 *
 * <p>Para principiantes: "atajo" de {@link NegocioException} para cuando se busca algo por id y no
 * existe. Ejemplo: {@code throw new RecursoNoEncontradoException("La clase 99 no existe");}
 */
public class RecursoNoEncontradoException extends NegocioException {

    public RecursoNoEncontradoException(String mensaje) {
        super(HttpStatus.NOT_FOUND, CodigosError.NO_ENCONTRADO, mensaje);
    }
}
