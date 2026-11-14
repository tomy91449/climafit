package ar.edu.utn.frc.climafit.commons.seguridad;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Convierte realm_access.roles del JWT de Keycloak en autoridades ROLE_*. */
public class KeycloakRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realm = jwt.getClaimAsMap("realm_access");
        List<GrantedAuthority> autoridades = new ArrayList<>();
        if (realm != null && realm.get("roles") instanceof Collection<?> roles) {
            for (Object rol : roles) {
                autoridades.add(new SimpleGrantedAuthority("ROLE_" + rol));
            }
        }
        return autoridades;
    }
}
