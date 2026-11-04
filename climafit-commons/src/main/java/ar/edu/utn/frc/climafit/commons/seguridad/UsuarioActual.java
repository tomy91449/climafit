package ar.edu.utn.frc.climafit.commons.seguridad;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Acceso cómodo al usuario que hizo la solicitud, para los controles de PROPIEDAD que el
 * gateway no puede resolver (porque no conoce los datos):
 * <ul>
 *   <li>RN05: solo el profesor dueño de la clase o un ADMIN la modifica.</li>
 *   <li>RN08: un ejercicio personalizado solo lo ve su dueño.</li>
 * </ul>
 * El id del usuario es el claim "sub" del JWT (un UUID que asigna Keycloak). Es lo que se guarda
 * en las tablas como profesor_id, alumno_id, propietario_id.
 */
public class UsuarioActual {

    public static final String ADMIN = "ADMIN";
    public static final String PROFESOR = "PROFESOR";
    public static final String ALUMNO = "ALUMNO";
    public static final String SERVICIO = "SERVICIO";

    /** "sub" del token: identificador estable del usuario en Keycloak. */
    public String id() {
        return token().getToken().getSubject();
    }

    /** Nombre de usuario legible (ej. "profe1"), útil para logs y mensajes. */
    public String nombreUsuario() {
        return token().getToken().getClaimAsString("preferred_username");
    }

    public boolean tieneRol(String rol) {
        return token().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + rol));
    }

    public boolean esAdmin() {
        return tieneRol(ADMIN);
    }

    /** true si el usuario actual es el dueño indicado o es ADMIN (patrón típico de RN05). */
    public boolean esDuenioOAdmin(String duenioId) {
        return esAdmin() || id().equals(duenioId);
    }

    private JwtAuthenticationToken token() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) {
            return jwt;
        }
        throw new IllegalStateException("No hay un usuario autenticado con JWT en esta solicitud");
    }
}
