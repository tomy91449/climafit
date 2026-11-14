package ar.edu.utn.frc.climafit.commons;

import ar.edu.utn.frc.climafit.commons.trazabilidad.CorrelationId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** El correlation id viaja en la respuesta y aparece en los errores. */
@SpringBootTest(classes = AplicacionDePrueba.class)
@AutoConfigureMockMvc
class TrazabilidadTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void sinCabeceraSeGeneraUnCorrelationIdNuevo() throws Exception {
        mvc.perform(get("/yo").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationId.CABECERA, matchesPattern("[0-9a-f-]{36}")));
    }

    @Test
    void conCabeceraSeRespetaElCorrelationIdRecibido() throws Exception {
        mvc.perform(get("/yo").with(jwt()).header(CorrelationId.CABECERA, "pedido-123"))
                .andExpect(header().string(CorrelationId.CABECERA, "pedido-123"));
    }

    @Test
    void unaCabeceraConFormatoInvalidoSeReemplaza() throws Exception {
        mvc.perform(get("/yo").with(jwt()).header(CorrelationId.CABECERA, "no válido con espacios"))
                .andExpect(header().string(CorrelationId.CABECERA, matchesPattern("[0-9a-f-]{36}")));
    }

    @Test
    void losErroresLlevanElCorrelationId() throws Exception {
        mvc.perform(get("/negocio").with(jwt()).header(CorrelationId.CABECERA, "pedido-456"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.correlationId").value("pedido-456"));
    }

    @Test
    void elDe401TambienLlevaElCorrelationId() throws Exception {
        mvc.perform(get("/yo").header(CorrelationId.CABECERA, "pedido-789"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.correlationId").value("pedido-789"));
    }
}
