package ar.edu.utn.frc.climafit.commons;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica que un servicio que solo agrega la dependencia de climafit-commons queda con
 * la seguridad y el formato de errores de la propuesta.
 */
@SpringBootTest(classes = AplicacionDePrueba.class)
@AutoConfigureMockMvc
class ConfiguracionComunTest {

    @Autowired
    private MockMvc mvc;

    /** La aplicación de prueba no habla con Keycloak: los tokens se simulan con jwt(). */
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void sinTokenResponde401EnFormatoComunConWwwAuthenticate() throws Exception {
        mvc.perform(get("/yo"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"))
                .andExpect(jsonPath("$.ruta").value("/yo"))
                .andExpect(jsonPath("$.fecha").exists());
    }

    @Test
    void conTokenExponeElSubYLosRoles() throws Exception {
        mvc.perform(get("/yo").with(jwt().jwt(j -> j.subject("profe-1"))
                        .authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("profe-1"))
                .andExpect(jsonPath("$.roles[0]").value("PROFESOR"));
    }

    @Test
    void sinElRolRequeridoResponde403EnFormatoComun() throws Exception {
        mvc.perform(get("/solo-admin").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ALUMNO"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    void conElRolRequeridoResponde200() throws Exception {
        mvc.perform(get("/solo-admin").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void rutaInternaSoloParaElRolServicio() throws Exception {
        mvc.perform(get("/internal/ping").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
        mvc.perform(get("/internal/ping").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SERVICIO"))))
                .andExpect(status().isOk());
    }

    @Test
    void datosInvalidosResponden400ConElDetalleDeCadaCampo() throws Exception {
        mvc.perform(post("/validar").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"\", \"porcentaje\": 150}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
                .andExpect(jsonPath("$.campos.nombre").exists())
                .andExpect(jsonPath("$.campos.porcentaje").exists());
    }

    @Test
    void errorDeNegocioRespondeSuStatusYCodigo() throws Exception {
        mvc.perform(get("/negocio").with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.codigo").value("CUPO_COMPLETO"))
                .andExpect(jsonPath("$.mensaje").value("La clase no tiene lugares disponibles"));
    }

    @Test
    void documentacionOpenApiEsLibre() throws Exception {
        // No hay springdoc en la aplicación de prueba: la ruta no existe (404), pero no pide token (no 401).
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }
}
