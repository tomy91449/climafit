package ar.edu.utn.frc.climafit.actividades;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * Base de los tests de integración: levanta ms-actividades completo contra H2 (perfil test),
 * con los datos iniciales de Flyway. Cada test corre en una transacción que se deshace al final,
 * así ningún test ve los cambios de otro.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class PruebaIntegracion {

    /** Los tokens se simulan con jwt(); no se habla con Keycloak. */
    @MockitoBean
    protected JwtDecoder jwtDecoder;

    protected static RequestPostProcessor admin() {
        return usuario("admin-1", "ADMIN");
    }

    protected static RequestPostProcessor profesor(String sub) {
        return usuario(sub, "PROFESOR");
    }

    protected static RequestPostProcessor alumno() {
        return usuario("alumno-1", "ALUMNO");
    }

    protected static RequestPostProcessor servicio() {
        return usuario("service-account-climafit-servicios", "SERVICIO");
    }

    protected static RequestPostProcessor usuario(String sub, String rol) {
        return jwt().jwt(token -> token.subject(sub)).authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }
}
