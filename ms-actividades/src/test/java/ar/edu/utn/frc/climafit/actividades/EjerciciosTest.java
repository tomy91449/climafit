package ar.edu.utn.frc.climafit.actividades;

import com.jayway.jsonpath.JsonPath;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Endpoints de /ejercicios de punta a punta (F04, RN07, RN08). */
class EjerciciosTest extends PruebaIntegracion {

    private static final String RESPIRACION = """
            {"nombre": "Respiración en parque", "descripcion": "Respiración diafragmática de pie, 3 minutos",
             "nivel": "INICIAL", "actividadesCompatibles": [1]}
            """;

    @Autowired
    private MockMvc mvc;

    @Test
    void listaLosEjerciciosBase() throws Exception {
        mvc.perform(get("/ejercicios").with(alumno()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)));
    }

    @Test
    void filtraPorActividad() throws Exception {
        mvc.perform(get("/ejercicios").param("actividadId", "1").with(profesor("profe-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].nombre", hasItem("Saludo al sol")));
    }

    @Test
    void rn08_elProfesorCreaUnPersonalizadoQueSoloVeEl() throws Exception {
        Long id = crearComoProfe1();

        mvc.perform(get("/ejercicios/" + id).with(profesor("profe-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propietarioId").value("profe-1"));
        mvc.perform(get("/ejercicios/" + id).with(profesor("profe-2")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));

        mvc.perform(get("/ejercicios").param("actividadId", "1").with(profesor("profe-1")))
                .andExpect(jsonPath("$", hasSize(3)));
        mvc.perform(get("/ejercicios").param("actividadId", "1").with(profesor("profe-2")))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void rn08_unProfesorNoModificaUnEjercicioBase() throws Exception {
        mvc.perform(put("/ejercicios/1").with(profesor("profe-1")).contentType(MediaType.APPLICATION_JSON)
                        .content(RESPIRACION))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    void rn08_elDuenioDaDeBajaSuEjercicioYDejaDeListarse() throws Exception {
        Long id = crearComoProfe1();

        mvc.perform(delete("/ejercicios/" + id).with(profesor("profe-2")))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/ejercicios/" + id).with(profesor("profe-1")))
                .andExpect(status().isNoContent());
        mvc.perform(get("/ejercicios").with(profesor("profe-1")))
                .andExpect(jsonPath("$[*].nombre", not(hasItem("Respiración en parque"))));
    }

    @Test
    void unAlumnoNoPuedeCrearEjercicios() throws Exception {
        mvc.perform(post("/ejercicios").with(alumno()).contentType(MediaType.APPLICATION_JSON).content(RESPIRACION))
                .andExpect(status().isForbidden());
    }

    @Test
    void unNivelInvalidoResponde400() throws Exception {
        String invalido = RESPIRACION.replace("INICIAL", "EXPERTO");
        mvc.perform(post("/ejercicios").with(profesor("profe-1")).contentType(MediaType.APPLICATION_JSON)
                        .content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
    }

    @Test
    void rn07_rutinaCompatibleConYoga() throws Exception {
        mvc.perform(post("/ejercicios/compatibilidad").with(servicio()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actividadId\": 1, \"ejercicioIds\": [1, 2]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compatible").value(true))
                .andExpect(jsonPath("$.incompatibles", hasSize(0)));
    }

    @Test
    void rn07_burpeesNoEsCompatibleConYoga() throws Exception {
        mvc.perform(post("/ejercicios/compatibilidad").with(profesor("profe-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actividadId\": 1, \"ejercicioIds\": [1, 3]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compatible").value(false))
                .andExpect(jsonPath("$.incompatibles[0]").value(3));
    }

    @Test
    void rn07_unAlumnoNoPuedeVerificarCompatibilidad() throws Exception {
        mvc.perform(post("/ejercicios/compatibilidad").with(alumno()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actividadId\": 1, \"ejercicioIds\": [1]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rn07_conUnaActividadInexistenteResponde404() throws Exception {
        mvc.perform(post("/ejercicios/compatibilidad").with(servicio()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actividadId\": 999, \"ejercicioIds\": [1]}"))
                .andExpect(status().isNotFound());
    }

    private Long crearComoProfe1() throws Exception {
        String respuesta = mvc.perform(post("/ejercicios").with(profesor("profe-1"))
                        .contentType(MediaType.APPLICATION_JSON).content(RESPIRACION))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.propietarioId").value("profe-1"))
                .andExpect(jsonPath("$.actividadesCompatibles[0]").value(1))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }
}
