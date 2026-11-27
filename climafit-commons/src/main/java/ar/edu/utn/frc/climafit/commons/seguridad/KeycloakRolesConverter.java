package ar.edu.utn.frc.climafit.commons.seguridad;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Convierte los roles de realm de Keycloak ({@code realm_access.roles} del JWT) en autoridades
 * de Spring Security con prefijo {@code ROLE_}: ADMIN, PROFESOR, ALUMNO y SERVICIO pasan a ser
 * ROLE_ADMIN, ROLE_PROFESOR, ROLE_ALUMNO y ROLE_SERVICIO.
 *
 * <p>Para principiantes: el JWT es un JSON firmado. Keycloak guarda los roles adentro así:
 * <pre>
 *   "realm_access": { "roles": ["PROFESOR", "offline_access", ...] }
 * </pre>
 * Spring Security no sabe leer ese formato propio de Keycloak; por defecto busca otro campo
 * ("scope"). Esta clase le enseña dónde están los roles. Además Spring espera que cada rol empiece
 * con "ROLE_": así {@code hasRole('PROFESOR')} encuentra la autoridad "ROLE_PROFESOR".
 *
 * <p>La usan el API Gateway (SeguridadGatewayConfig) y los microservicios
 * (ClimafitSeguridadAutoConfiguration), para que todos lean los roles de la misma forma.
 */
public class KeycloakRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    // Nombres de los campos dentro del token y el prefijo que exige Spring Security.
    private static final String CLAIM_REALM_ACCESS = "realm_access";
    private static final String CLAIM_ROLES = "roles";
    private static final String PREFIJO = "ROLE_";

    /** Spring llama a este método con cada token ya validado (firma, emisor y vencimiento correctos). */
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        // 1. Buscar el bloque "realm_access". Si no está o no es un objeto, el usuario no tiene roles.
        Object realmAccess = jwt.getClaims().get(CLAIM_REALM_ACCESS);
        if (!(realmAccess instanceof Map<?, ?> mapa)) {
            return List.of();
        }
        // 2. Dentro de ese bloque, buscar la lista "roles".
        Object roles = mapa.get(CLAIM_ROLES);
        if (!(roles instanceof Collection<?> coleccion)) {
            return List.of();
        }
        // 3. Por cada rol de texto, crear la autoridad "ROLE_<rol>" que entiende Spring Security.
        return coleccion.stream()
                .filter(String.class::isInstance)   // ignorar cualquier valor que no sea texto
                .map(String.class::cast)
                .map(rol -> (GrantedAuthority) new SimpleGrantedAuthority(PREFIJO + rol))
                .toList();
    }
}
