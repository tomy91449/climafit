package ar.edu.utn.frc.climafit.commons.seguridad;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRolesConverterTest {

    private final KeycloakRolesConverter converter = new KeycloakRolesConverter();

    @Test
    void convierteLosRolesDeRealmEnAutoridadesConPrefijo() {
        Jwt jwt = jwtCon(Map.of("realm_access", Map.of("roles", List.of("PROFESOR", "offline_access"))));

        assertThat(converter.convert(jwt))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_PROFESOR", "ROLE_offline_access");
    }

    @Test
    void sinRealmAccessDevuelveListaVacia() {
        Jwt jwt = jwtCon(Map.of("otro", "valor"));

        assertThat(converter.convert(jwt)).isEmpty();
    }

    @Test
    void rolesConFormatoInesperadoSeIgnoran() {
        Jwt jwt = jwtCon(Map.of("realm_access", Map.of("roles", "ADMIN")));

        assertThat(converter.convert(jwt)).isEmpty();
    }

    private static Jwt jwtCon(Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("usuario-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60));
        claims.forEach(builder::claim);
        return builder.build();
    }
}
