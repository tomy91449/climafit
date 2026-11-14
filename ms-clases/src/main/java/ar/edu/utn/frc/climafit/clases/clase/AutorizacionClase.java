package ar.edu.utn.frc.climafit.clases.clase;

import ar.edu.utn.frc.climafit.commons.error.AccesoDenegadoException;
import org.springframework.stereotype.Component;

/**
 * RN05: los endpoints "Profesor dueño" comparan el sub del token con profesor_id de la clase.
 * Si no coincide (y no es ADMIN, donde corresponde), 403 ACCESO_DENEGADO.
 * Es público para que la Persona 4 lo use en sus endpoints de evaluación y decisión de riesgo.
 */
@Component
public class AutorizacionClase {

    /** Endpoints "Profesor dueño / ADMIN". */
    public void verificarDuenioOAdmin(Clase clase, String usuarioSub, boolean esAdmin) { // RN05
        if (!esAdmin && !clase.getProfesorId().equals(usuarioSub)) {
            throw new AccesoDenegadoException("La clase pertenece a otro profesor");
        }
    }

    /** Endpoints "Profesor dueño" a secas: ni siquiera el ADMIN. */
    public void verificarDuenio(Clase clase, String usuarioSub) { // RN05
        if (!clase.getProfesorId().equals(usuarioSub)) {
            throw new AccesoDenegadoException("La clase pertenece a otro profesor");
        }
    }
}
