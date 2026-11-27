package ar.edu.utn.frc.climafit.commons.error;

import org.springframework.http.HttpStatus;

/**
 * 403 ACCESO_DENEGADO: el usuario está autenticado pero no puede operar sobre ese recurso.
 *
 * <p>Para principiantes: es un "atajo" de {@link NegocioException} con el status y el código ya
 * puestos. Se usa en los controles de dueño que el gateway no puede hacer, por ejemplo cuando
 * profe2 intenta cancelar una clase de profe1 (RN05): tiene el rol correcto, pero la clase no es suya.
 */
public class AccesoDenegadoException extends NegocioException {

    public AccesoDenegadoException(String mensaje) {
        super(HttpStatus.FORBIDDEN, CodigosError.ACCESO_DENEGADO, mensaje);
    }
}
