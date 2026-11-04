package ar.edu.utn.frc.climafit.commons.seguridad;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRolesConverterTest {

    private final KeycloakRolesConverter converter = new KeycloakRolesConverter();

    @Test
    void convierteLosRolesDelRealmEnAutoridadesConPrefijoRole() {
        Jwt jwt = jwtCon(Map.of("realm_access", Map.of("roles", List.of("PROFESOR", "default-roles-climafit"))));

        assertThat(converter.convert(jwt))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_PROFESOR", "ROLE_default-roles-climafit");
    }

    @Test
    void sinRealmAccessNoTieneAutoridades() {
        Jwt jwt = jwtCon(Map.of("otro", "valor"));

        assertThat(converter.convert(jwt)).isEmpty();
    }

    private static Jwt jwtCon(Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "RS256").subject("usuario-1");
        claims.forEach(builder::claim);
        return builder.build();
    }
}
