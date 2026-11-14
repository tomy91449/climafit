package ar.edu.utn.frc.climafit.clases.evaluacion;

import ar.edu.utn.frc.climafit.commons.error.NegocioException;
import org.springframework.http.HttpStatus;

/** RN12: 409 CLASE_NO_EN_RIESGO. */
public class ClaseNoEnRiesgoException extends NegocioException {

    public ClaseNoEnRiesgoException(String mensaje) {
        super("CLASE_NO_EN_RIESGO", mensaje, HttpStatus.CONFLICT);
    }
}
