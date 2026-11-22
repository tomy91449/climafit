package ar.edu.utn.frc.climafit.gateway.seguridad;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Mismo criterio que el KeycloakRolesConverter de climafit-commons: los roles de realm
 * ({@code realm_access.roles} del JWT) pasan a ser autoridades ROLE_ADMIN, ROLE_PROFESOR, etc.
 * Así el gateway y los microservicios interpretan los roles exactamente igual.
 */
public class KeycloakRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String PREFIJO = "ROLE_";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        if (!(jwt.getClaims().get("realm_access") instanceof Map<?, ?> realmAccess)
                || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }
        return roles.stream()
                .filter(String.class::isInstance)
                .map(rol -> (GrantedAuthority) new SimpleGrantedAuthority(PREFIJO + rol))
                .toList();
    }
}
