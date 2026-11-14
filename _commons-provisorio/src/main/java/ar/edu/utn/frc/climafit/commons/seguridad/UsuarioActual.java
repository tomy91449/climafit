package ar.edu.utn.frc.climafit.commons.seguridad;

import ar.edu.utn.frc.climafit.commons.error.AccesoDenegadoException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Set;
import java.util.stream.Collectors;

/** Expone el sub y los roles del token (para RN05 y RN08). */
public class UsuarioActual {

    public String sub() {
        Authentication auth = autenticacion();
        if (auth.getPrincipal() instanceof Jwt jwt && jwt.getSubject() != null) {
            return jwt.getSubject();
        }
        return auth.getName();
    }

    /** Roles sin el prefijo ROLE_ (ADMIN, PROFESOR, ALUMNO, SERVICIO). */
    public Set<String> roles() {
        return autenticacion().getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .collect(Collectors.toSet());
    }

    public boolean tieneRol(String rol) {
        return roles().contains(rol);
    }

    private Authentication autenticacion() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccesoDenegadoException("No hay un usuario autenticado");
        }
        return auth;
    }
}
