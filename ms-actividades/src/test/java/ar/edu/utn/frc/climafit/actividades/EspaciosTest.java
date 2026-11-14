package ar.edu.utn.frc.climafit.actividades;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Endpoints de /espacios (F03). */
class EspaciosTest extends PruebaIntegracion {

    private static final String COSTANERA = """
            {"nombre": "Costanera Río Suquía", "direccion": "Costanera y Av. Poeta Lugones",
             "latitud": -31.4135, "longitud": -64.1811, "alAireLibre": true}
            """;

    @Autowired
    private MockMvc mvc;

    @Test
    void listaLosEspaciosIniciales() throws Exception {
        mvc.perform(get("/espacios").with(alumno()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void consultaUnEspacioConCoordenadasYSiEsAlAireLibre() throws Exception {
        mvc.perform(get("/espacios/1").with(servicio()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Parque Sarmiento"))
                .andExpect(jsonPath("$.latitud").value(-31.429))
                .andExpect(jsonPath("$.longitud").value(-64.18))
                .andExpect(jsonPath("$.alAireLibre").value(true));

        mvc.perform(get("/espacios/3").with(servicio()))
                .andExpect(jsonPath("$.alAireLibre").value(false));
    }

    @Test
    void elAdminRegistraUnEspacio() throws Exception {
        mvc.perform(post("/espacios").with(admin()).contentType(MediaType.APPLICATION_JSON).content(COSTANERA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.alAireLibre").value(true));
    }

    @Test
    void unProfesorNoPuedeRegistrarEspacios() throws Exception {
        mvc.perform(post("/espacios").with(profesor("profe-1")).contentType(MediaType.APPLICATION_JSON)
                        .content(COSTANERA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    void coordenadasFueraDeRangoResponden400() throws Exception {
        String invalido = """
                {"nombre": "Fuera del mapa", "latitud": -95, "longitud": 200, "alAireLibre": true}
                """;
        mvc.perform(post("/espacios").with(admin()).contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
    }

    @Test
    void elAdminActualizaUnEspacio() throws Exception {
        String gimnasioAlAireLibre = """
                {"nombre": "Gimnasio Centro", "direccion": "Terraza", "latitud": -31.4167,
                 "longitud": -64.1833, "alAireLibre": true}
                """;
        mvc.perform(put("/espacios/3").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content(gimnasioAlAireLibre))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alAireLibre").value(true));
    }

    @Test
    void unEspacioInexistenteResponde404() throws Exception {
        mvc.perform(get("/espacios/999").with(alumno()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }
}
