package ar.edu.utn.frc.climafit.commons.error;

import org.springframework.http.HttpStatus;

/**
 * 403 por un control de PROPIEDAD que hace el propio servicio (RN05: solo el profesor dueño
 * de la clase o un admin). Los 403 por ROL los resuelve Spring Security antes de llegar acá.
 */
public class AccesoDenegadoException extends NegocioException {

    public AccesoDenegadoException(String mensaje) {
        super("ACCESO_DENEGADO", mensaje, HttpStatus.FORBIDDEN);
    }
}
