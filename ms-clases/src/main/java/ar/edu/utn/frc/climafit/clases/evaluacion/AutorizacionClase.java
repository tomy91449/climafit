package ar.edu.utn.frc.climafit.clases.evaluacion;

import ar.edu.utn.frc.climafit.clases.clase.Clase;
import ar.edu.utn.frc.climafit.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.climafit.commons.seguridad.UsuarioActual;
import org.springframework.stereotype.Component;

/** RN05: solo el profesor de la clase o un administrador. 403 ACCESO_DENEGADO si no. */
@Component
public class AutorizacionClase {

    private final UsuarioActual usuario;

    public AutorizacionClase(UsuarioActual usuario) {
        this.usuario = usuario;
    }

    /** Profesor dueño o ADMIN (evaluar, consultar evaluaciones). */
    public void verificarProfesorDuenoOAdmin(Clase clase) {
        if (!usuario.esAdmin() && !clase.getProfesorId().equals(usuario.id())) {
            throw new AccesoDenegadoException("Solo el profesor de la clase o un administrador pueden hacerlo");
        }
    }

    /** Solo el profesor dueño (decisión sobre una clase en riesgo: ni siquiera el ADMIN). */
    public void verificarProfesorDueno(Clase clase) {
        if (!clase.getProfesorId().equals(usuario.id())) {
            throw new AccesoDenegadoException("Solo el profesor de la clase puede decidir sobre ella");
        }
    }
}
