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
 */
public class KeycloakRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String CLAIM_REALM_ACCESS = "realm_access";
    private static final String CLAIM_ROLES = "roles";
    private static final String PREFIJO = "ROLE_";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Object realmAccess = jwt.getClaims().get(CLAIM_REALM_ACCESS);
        if (!(realmAccess instanceof Map<?, ?> mapa)) {
            return List.of();
        }
        Object roles = mapa.get(CLAIM_ROLES);
        if (!(roles instanceof Collection<?> coleccion)) {
            return List.of();
        }
        return coleccion.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(rol -> (GrantedAuthority) new SimpleGrantedAuthority(PREFIJO + rol))
                .toList();
    }
}
