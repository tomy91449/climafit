package ar.edu.utn.frc.climafit.commons.seguridad;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Keycloak guarda los roles del realm dentro del JWT así:
 * <pre>
 * "realm_access": { "roles": ["PROFESOR", "default-roles-climafit", ...] }
 * </pre>
 * Spring Security no sabe leer ese formato: por defecto busca el claim "scope". Este conversor
 * transforma cada rol en una autoridad "ROLE_PROFESOR", que es lo que esperan
 * {@code hasRole("PROFESOR")} y {@code @PreAuthorize("hasRole('PROFESOR')")}.
 */
public class KeycloakRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }
        return roles.stream()
                .map(rol -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + rol))
                .toList();
    }
}
