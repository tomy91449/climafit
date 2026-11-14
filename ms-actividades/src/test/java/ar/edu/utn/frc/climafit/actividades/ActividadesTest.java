package ar.edu.utn.frc.climafit.actividades;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Endpoints de /actividades (F02). */
class ActividadesTest extends PruebaIntegracion {

    private static final String PILATES = """
            {"nombre": "Pilates", "descripcion": "Pilates de suelo al aire libre",
             "probLluviaMax": 60, "lluviaMaxMm": 0.5, "vientoMaxKmh": 25, "tempMinC": 12}
            """;

    @Autowired
    private MockMvc mvc;

    @Test
    void sinTokenResponde401() throws Exception {
        mvc.perform(get("/actividades"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    void cualquierUsuarioAutenticadoListaLasActividadesIniciales() throws Exception {
        mvc.perform(get("/actividades").with(alumno()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[*].nombre", hasItem("Yoga")))
                .andExpect(jsonPath("$[*].nombre", hasItem("Acrobacia aérea")))
                .andExpect(jsonPath("$[*].nombre", hasItem("Patinaje")));
    }

    @Test
    void consultaUnaActividadConSusCriteriosClimaticos() throws Exception {
        mvc.perform(get("/actividades/1").with(profesor("profe-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Yoga"))
                .andExpect(jsonPath("$.probLluviaMax").value(80))
                .andExpect(jsonPath("$.lluviaMaxMm").value(0.5))
                .andExpect(jsonPath("$.vientoMaxKmh").value(35))
                .andExpect(jsonPath("$.tempMinC").value(5))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void acrobaciaAereaEsLaMasSensibleAlVientoYPatinajeNoToleraLluvia() throws Exception {
        mvc.perform(get("/actividades/4").with(servicio()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Acrobacia aérea"))
                .andExpect(jsonPath("$.vientoMaxKmh").value(20));
        mvc.perform(get("/actividades/5").with(servicio()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Patinaje"))
                .andExpect(jsonPath("$.probLluviaMax").value(80))
                .andExpect(jsonPath("$.lluviaMaxMm").value(0.1))
                .andExpect(jsonPath("$.vientoMaxKmh").value(60));
    }

    @Test
    void elRolServicioTambienPuedeConsultar() throws Exception {
        mvc.perform(get("/actividades/3").with(servicio()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Running"));
    }

    @Test
    void unaActividadInexistenteResponde404() throws Exception {
        mvc.perform(get("/actividades/999").with(alumno()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    void elAdminRegistraUnaActividad() throws Exception {
        mvc.perform(post("/actividades").with(admin()).contentType(MediaType.APPLICATION_JSON).content(PILATES))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Pilates"))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void unAlumnoNoPuedeRegistrarActividades() throws Exception {
        mvc.perform(post("/actividades").with(alumno()).contentType(MediaType.APPLICATION_JSON).content(PILATES))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    void unProfesorNoPuedeRegistrarActividades() throws Exception {
        mvc.perform(post("/actividades").with(profesor("profe-1")).contentType(MediaType.APPLICATION_JSON)
                        .content(PILATES))
                .andExpect(status().isForbidden());
    }

    @Test
    void criteriosInvalidosResponden400() throws Exception {
        String invalida = """
                {"nombre": "Invalida", "descripcion": "x", "probLluviaMax": 150, "lluviaMaxMm": -1,
                 "vientoMaxKmh": -5, "tempMinC": 5}
                """;
        mvc.perform(post("/actividades").with(admin()).contentType(MediaType.APPLICATION_JSON).content(invalida))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
                .andExpect(jsonPath("$.campos.lluviaMaxMm").exists());
    }

    @Test
    void unNombreRepetidoResponde400() throws Exception {
        String repetida = """
                {"nombre": "yoga", "probLluviaMax": 10, "lluviaMaxMm": 1, "vientoMaxKmh": 10, "tempMinC": 10}
                """;
        mvc.perform(post("/actividades").with(admin()).contentType(MediaType.APPLICATION_JSON).content(repetida))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
    }

    @Test
    void elAdminActualizaLosCriterios() throws Exception {
        String yogaCambiado = """
                {"nombre": "Yoga", "descripcion": "Yoga al aire libre",
                 "probLluviaMax": 70, "lluviaMaxMm": 1.0, "vientoMaxKmh": 20, "tempMinC": 14}
                """;
        mvc.perform(put("/actividades/1").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content(yogaCambiado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.probLluviaMax").value(70))
                .andExpect(jsonPath("$.lluviaMaxMm").value(1.0))
                .andExpect(jsonPath("$.tempMinC").value(14));
    }

    @Test
    void laBajaEsLogicaYLaActividadDejaDeListarse() throws Exception {
        mvc.perform(delete("/actividades/3").with(admin()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/actividades").with(alumno()))
                .andExpect(jsonPath("$[*].nombre", not(hasItem("Running"))));
        mvc.perform(get("/actividades/3").with(alumno()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }
}
