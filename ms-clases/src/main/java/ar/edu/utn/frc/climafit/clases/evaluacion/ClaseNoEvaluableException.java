package ar.edu.utn.frc.climafit.clases.evaluacion;

import ar.edu.utn.frc.climafit.commons.error.NegocioException;
import org.springframework.http.HttpStatus;

/** RN09: 409 CLASE_NO_EVALUABLE. */
public class ClaseNoEvaluableException extends NegocioException {

    public ClaseNoEvaluableException(String mensaje) {
        super(HttpStatus.CONFLICT, "CLASE_NO_EVALUABLE", mensaje);
    }
}
