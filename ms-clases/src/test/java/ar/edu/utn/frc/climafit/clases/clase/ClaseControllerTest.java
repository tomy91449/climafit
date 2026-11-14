package ar.edu.utn.frc.climafit.clases.clase;

import ar.edu.utn.frc.climafit.clases.BaseIntegracionTest;
import ar.edu.utn.frc.climafit.clases.estado.EstadoClase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Prueba la capa HTTP: roles, códigos de estado y formato de error. */
@AutoConfigureMockMvc
class ClaseControllerTest extends BaseIntegracionTest {

    @Autowired MockMvc mvc;

    private static RequestPostProcessor como(String sub, String rol) {
        return jwt().jwt(j -> j.subject(sub)).authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private String cuerpoClase(int cupo) {
        return """
                {"tipoActividadId":1,"espacioId":1,"inicio":"%s","duracionMinutos":60,"cupo":%d,
                 "rutina":[{"ejercicioId":1,"orden":1,"indicaciones":"Calentar"}]}
                """.formatted(manana(18), cupo);
    }

    @Test
    void sinTokenResponde401ConElFormatoComun() throws Exception {
        mvc.perform(get("/clases/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void unAlumnoNoPuedeProgramarClases() throws Exception {
        mvc.perform(post("/clases").with(como(ALUMNO_1, "ALUMNO"))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpoClase(10)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    void unProfesorProgramaUnaClaseYResponde201() throws Exception {
        mvc.perform(post("/clases").with(como(PROFE_1, "PROFESOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpoClase(10)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.estado").value("PROGRAMADA"))
                .andExpect(jsonPath("$.profesorId").value(PROFE_1))
                .andExpect(jsonPath("$.alAireLibre").value(true))
                .andExpect(jsonPath("$.lugaresDisponibles").value(10))
                .andExpect(jsonPath("$.rutina[0].ejercicioId").value(1));
    }

    @Test
    void unCuerpoIncompletoResponde400DatosInvalidos() throws Exception {
        mvc.perform(post("/clases").with(como(PROFE_1, "PROFESOR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cupo\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
    }

    @Test
    void rn07_unEjercicioIncompatibleResponde422() throws Exception {
        String cuerpo = """
                {"tipoActividadId":1,"espacioId":1,"inicio":"%s","duracionMinutos":60,"cupo":10,
                 "rutina":[{"ejercicioId":3,"orden":1}]}
                """.formatted(manana(18));

        mvc.perform(post("/clases").with(como(PROFE_1, "PROFESOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("EJERCICIO_INCOMPATIBLE"));
    }

    @Test
    void rn04_unaClaseSuperpuestaResponde409() throws Exception {
        mvc.perform(post("/clases").with(como(PROFE_1, "PROFESOR"))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpoClase(10)));

        mvc.perform(post("/clases").with(como(PROFE_1, "PROFESOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpoClase(10)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SUPERPOSICION_PROFESOR"));
    }

    @Test
    void rn05_unProfesorAjenoNoPuedeCancelarYRecibe403() throws Exception {
        Clase clase = claseEnBase(PROFE_1, manana(18), 60, 10, EstadoClase.PROGRAMADA);

        mvc.perform(patch("/clases/{id}/cancelacion", clase.getId()).with(como(PROFE_2, "PROFESOR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"x\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    void elProfesorDuenoCancelaYLaRespuestaTraeElEstado() throws Exception {
        Clase clase = claseEnBase(PROFE_1, manana(18), 60, 10, EstadoClase.PROGRAMADA);

        mvc.perform(patch("/clases/{id}/cancelacion", clase.getId()).with(como(PROFE_1, "PROFESOR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Profesor enfermo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
    }

    @Test
    void laCancelacionSinMotivoResponde400() throws Exception {
        Clase clase = claseEnBase(PROFE_1, manana(18), 60, 10, EstadoClase.PROGRAMADA);

        mvc.perform(patch("/clases/{id}/cancelacion", clase.getId()).with(como(PROFE_1, "PROFESOR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
    }

    @Test
    void unaClaseInexistenteResponde404ConElFormatoComun() throws Exception {
        mvc.perform(get("/clases/999999").with(como(ALUMNO_1, "ALUMNO")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"))
                .andExpect(jsonPath("$.ruta").value("/clases/999999"));
    }

    @Test
    void elAlumnoSeInscribeYElCupoSeDescuenta() throws Exception {
        Clase clase = claseEnBase(PROFE_1, manana(18), 60, 3, EstadoClase.PROGRAMADA);

        mvc.perform(post("/clases/{id}/inscripciones", clase.getId()).with(como(ALUMNO_1, "ALUMNO")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.alumnoId").value(ALUMNO_1));

        mvc.perform(get("/clases/{id}", clase.getId()).with(como(ALUMNO_1, "ALUMNO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lugaresDisponibles").value(2));
    }

    @Test
    void rn01_conCupoCompletoResponde409() throws Exception {
        Clase clase = claseEnBase(PROFE_1, manana(18), 60, 1, EstadoClase.PROGRAMADA);
        inscripcionEnBase(clase.getId(), ALUMNO_1);

        mvc.perform(post("/clases/{id}/inscripciones", clase.getId()).with(como(ALUMNO_3, "ALUMNO")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CUPO_COMPLETO"))
                .andExpect(jsonPath("$.mensaje").value("La clase no tiene lugares disponibles"));
    }

    @Test
    void unProfesorNoPuedeInscribirse() throws Exception {
        Clase clase = claseEnBase(PROFE_1, manana(18), 60, 3, EstadoClase.PROGRAMADA);

        mvc.perform(post("/clases/{id}/inscripciones", clase.getId()).with(como(PROFE_2, "PROFESOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void elHistorialSoloLoVeElDuenoOElAdmin() throws Exception {
        Clase clase = claseEnBase(PROFE_1, manana(18), 60, 3, EstadoClase.PROGRAMADA);

        mvc.perform(get("/clases/{id}/historial", clase.getId()).with(como(PROFE_1, "PROFESOR")))
                .andExpect(status().isOk());
        mvc.perform(get("/clases/{id}/historial", clase.getId()).with(como("admin1-sub", "ADMIN")))
                .andExpect(status().isOk());
        mvc.perform(get("/clases/{id}/historial", clase.getId()).with(como(PROFE_2, "PROFESOR")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/clases/{id}/historial", clase.getId()).with(como(ALUMNO_1, "ALUMNO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void elHistorialMuestraProgramadaSuspendidaSinFechaReprogramada() throws Exception {
        mvc.perform(post("/clases").with(como(PROFE_1, "PROFESOR"))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpoClase(10)));
        Clase clase = clases.findAll().get(0);
        // la clase queda SUSPENDIDA_SIN_FECHA (RN13, sin día viable) y el profesor la reagenda (F06)
        clase.setEstado(EstadoClase.SUSPENDIDA_SIN_FECHA);
        clases.save(clase);

        mvc.perform(patch("/clases/{id}/reprogramacion", clase.getId()).with(como(PROFE_1, "PROFESOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevoInicio\":\"%s\"}".formatted(manana(18).plusDays(2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("REPROGRAMADA"));

        mvc.perform(get("/clases/{id}/historial", clase.getId()).with(como(PROFE_1, "PROFESOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estadoNuevo").value("PROGRAMADA"))
                .andExpect(jsonPath("$[1].estadoAnterior").value("SUSPENDIDA_SIN_FECHA"))
                .andExpect(jsonPath("$[1].estadoNuevo").value("REPROGRAMADA"))
                .andExpect(jsonPath("$[1].inicioNuevo").exists());
    }
}
