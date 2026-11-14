package ar.edu.utn.frc.climafit.commons.seguridad;

import ar.edu.utn.frc.climafit.commons.error.CodigosError;
import ar.edu.utn.frc.climafit.commons.error.NegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Datos del usuario del pedido actual, tomados del JWT: su {@code sub} y sus roles.
 * Se usa para los controles de propiedad (RN05, RN08): "¿esta clase / este ejercicio es de quien pide?".
 */
public class UsuarioActual {

    private static final String PREFIJO = "ROLE_";

    /** El {@code sub} del token. Falla con 401 si no hay usuario autenticado. */
    public String id() {
        Authentication autenticacion = autenticacion();
        if (autenticacion == null || !autenticacion.isAuthenticated()) {
            throw new NegocioException(HttpStatus.UNAUTHORIZED, CodigosError.NO_AUTENTICADO,
                    "Se requiere un usuario autenticado");
        }
        return autenticacion.getName();
    }

    /** Roles del usuario sin el prefijo ROLE_ (por ejemplo, ADMIN o PROFESOR). */
    public Set<String> roles() {
        Authentication autenticacion = autenticacion();
        if (autenticacion == null) {
            return Set.of();
        }
        return autenticacion.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(autoridad -> autoridad.startsWith(PREFIJO))
                .map(autoridad -> autoridad.substring(PREFIJO.length()))
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean tieneRol(String rol) {
        return roles().contains(rol);
    }

    public boolean esAdmin() {
        return tieneRol(Roles.ADMIN);
    }

    public boolean esProfesor() {
        return tieneRol(Roles.PROFESOR);
    }

    public boolean esAlumno() {
        return tieneRol(Roles.ALUMNO);
    }

    public boolean esServicio() {
        return tieneRol(Roles.SERVICIO);
    }

    private Authentication autenticacion() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
