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
 *
 * <p>Para principiantes: cuando llega un pedido, Spring Security valida el token y guarda el
 * resultado en el {@link SecurityContextHolder} (una especie de "bolsillo" propio de cada pedido).
 * Esta clase lee de ese bolsillo para responder "¿quién está haciendo este pedido?".
 *
 * <p>Los controllers la reciben por el constructor (Spring la crea en ClimafitSeguridadAutoConfiguration).
 * Ejemplo en ms-clases: {@code servicio.crear(cuerpo, usuario.id())} guarda quién es el profesor dueño.
 */
public class UsuarioActual {

    // Spring guarda los roles como "ROLE_PROFESOR"; acá se devuelven sin el prefijo: "PROFESOR".
    private static final String PREFIJO = "ROLE_";

    /**
     * El {@code sub} del token: el identificador del usuario en Keycloak.
     * Es lo que se guarda en la base como profesor_id o alumno_id.
     * Falla con 401 si no hay usuario autenticado.
     */
    public String id() {
        Authentication autenticacion = autenticacion();
        if (autenticacion == null || !autenticacion.isAuthenticated()) {
            throw new NegocioException(HttpStatus.UNAUTHORIZED, CodigosError.NO_AUTENTICADO,
                    "Se requiere un usuario autenticado");
        }
        // Con un JWT, getName() devuelve el campo "sub" del token.
        return autenticacion.getName();
    }

    /** Roles del usuario sin el prefijo ROLE_ (por ejemplo, ADMIN o PROFESOR). */
    public Set<String> roles() {
        Authentication autenticacion = autenticacion();
        if (autenticacion == null) {
            return Set.of();
        }
        // Las "autoridades" las creó KeycloakRolesConverter a partir del token.
        return autenticacion.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)                       // "ROLE_PROFESOR"
                .filter(autoridad -> autoridad.startsWith(PREFIJO))        // solo los roles
                .map(autoridad -> autoridad.substring(PREFIJO.length()))   // "PROFESOR"
                .collect(Collectors.toUnmodifiableSet());
    }

    /** ¿El usuario tiene este rol? Ejemplo: {@code tieneRol(Roles.ADMIN)}. */
    public boolean tieneRol(String rol) {
        return roles().contains(rol);
    }

    // Atajos para preguntar por cada rol. Se usan en los controles de negocio, por ejemplo:
    // "el admin puede cancelar cualquier clase; el profesor, solo las suyas".

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

    /** Lee la autenticación del pedido actual (la dejó ahí Spring Security al validar el JWT). */
    private Authentication autenticacion() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
