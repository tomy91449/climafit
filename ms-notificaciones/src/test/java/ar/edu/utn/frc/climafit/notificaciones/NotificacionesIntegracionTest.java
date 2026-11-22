package ar.edu.utn.frc.climafit.notificaciones;

import ar.edu.utn.frc.climafit.notificaciones.dominio.EstadoNotificacion;
import ar.edu.utn.frc.climafit.notificaciones.repositorio.NotificacionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de integración: levanta el servicio completo (seguridad, validaciones, Flyway, JPA y el
 * envío asincrónico con el proveedor "log") contra una base H2 en memoria.
 * <p>
 * Acá se prueba RN15 con el endpoint interno, que la colección de Postman no puede llamar porque
 * el gateway no lo expone (por diseño).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificacionesIntegracionTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private NotificacionRepository notificaciones;

    private static RequestPostProcessor usuario(String sub, String rol) {
        return jwt().jwt(j -> j.subject(sub)).authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private static final RequestPostProcessor SERVICIO = usuario("service-account-climafit-servicios", "SERVICIO");

    private static String evento(String eventoId, long claseId, String profesor, String destinatarios) {
        return """
                {"eventoId": "%s", "tipo": "SUSPENSION", "claseId": %d, "profesorId": "%s",
                 "mensaje": "Tu clase se suspendió por lluvia", "destinatarios": [%s]}
                """.formatted(eventoId, claseId, profesor, destinatarios);
    }

    @Test
    void rn15ElMismoEventoRepetidoNoDuplicaNotificaciones() throws Exception {
        String cuerpo = evento("clase-100-cambio-1", 100, "profe-a", "\"alu-1\", \"alu-2\"");

        mvc.perform(post("/internal/notificaciones").with(SERVICIO)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.notificacionesCreadas").value(2))
                .andExpect(jsonPath("$.duplicadasIgnoradas").value(0));

        // ms-clases reintenta el mismo pedido: se ignora sin error.
        mvc.perform(post("/internal/notificaciones").with(SERVICIO)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.notificacionesCreadas").value(0))
                .andExpect(jsonPath("$.duplicadasIgnoradas").value(2));

        assertThat(notificaciones.findByClase(100L)).hasSize(2);
    }

    @Test
    void rn14SeEnviaAlQueAceptoYSeOmiteAlQueNo() throws Exception {
        mvc.perform(put("/contactos/mio").with(usuario("alu-acepta", "ALUMNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telegramChatId\": \"100000001\", \"aceptaTelegram\": true}"))
                .andExpect(status().isOk());
        mvc.perform(put("/contactos/mio").with(usuario("alu-rechaza", "ALUMNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telegramChatId\": \"100000002\", \"aceptaTelegram\": false}"))
                .andExpect(status().isOk());

        mvc.perform(post("/internal/notificaciones").with(SERVICIO).contentType(MediaType.APPLICATION_JSON)
                        .content(evento("clase-200-cambio-1", 200, "profe-a", "\"alu-acepta\", \"alu-rechaza\"")))
                .andExpect(status().isAccepted());

        // El envío es asincrónico: se espera (como mucho 5 s) a que no quede ninguna PENDIENTE.
        esperarHasta(() -> notificaciones.findByClase(200L).stream().noneMatch(n -> n.estaPendiente()));

        var porUsuario = notificaciones.findByClase(200L);
        assertThat(porUsuario).anySatisfy(n -> {
            assertThat(n.getUsuarioId()).isEqualTo("alu-acepta");
            assertThat(n.getEstado()).isEqualTo(EstadoNotificacion.ENVIADA);
        });
        assertThat(porUsuario).anySatisfy(n -> {
            assertThat(n.getUsuarioId()).isEqualTo("alu-rechaza");
            assertThat(n.getEstado()).isEqualTo(EstadoNotificacion.OMITIDA);
        });
    }

    @Test
    void elEndpointInternoExigeRolServicio() throws Exception {
        mvc.perform(post("/internal/notificaciones").with(usuario("profe-a", "PROFESOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evento("clase-300-cambio-1", 300, "profe-a", "\"alu-1\"")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    void rn05UnProfesorNoVeLasNotificacionesDeClasesAjenas() throws Exception {
        mvc.perform(post("/internal/notificaciones").with(SERVICIO).contentType(MediaType.APPLICATION_JSON)
                        .content(evento("clase-400-cambio-1", 400, "profe-duenio", "\"alu-1\"")))
                .andExpect(status().isAccepted());

        mvc.perform(get("/notificaciones").param("claseId", "400").with(usuario("profe-duenio", "PROFESOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tipo").value("SUSPENSION"));

        mvc.perform(get("/notificaciones").param("claseId", "400").with(usuario("otro-profe", "PROFESOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));

        mvc.perform(get("/notificaciones").param("claseId", "400").with(usuario("admin", "ADMIN")))
                .andExpect(status().isOk());

        mvc.perform(get("/notificaciones").param("claseId", "400").with(usuario("alu-1", "ALUMNO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void chatIdInvalidoDevuelve400() throws Exception {
        mvc.perform(put("/contactos/mio").with(usuario("alu-x", "ALUMNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telegramChatId\": \"+5493511234567\", \"aceptaTelegram\": true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
    }

    @Test
    void aceptarSinChatIdDevuelve400() throws Exception {
        mvc.perform(put("/contactos/mio").with(usuario("alu-y", "ALUMNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aceptaTelegram\": true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
    }

    @Test
    void consultarContactoPropio() throws Exception {
        mvc.perform(get("/contactos/mio").with(usuario("alu-nuevo", "ALUMNO")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));

        mvc.perform(put("/contactos/mio").with(usuario("alu-nuevo", "ALUMNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telegramChatId\": \"987654321\", \"aceptaTelegram\": true}"))
                .andExpect(status().isOk());

        mvc.perform(get("/contactos/mio").with(usuario("alu-nuevo", "ALUMNO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telegramChatId").value("987654321"))
                .andExpect(jsonPath("$.aceptaTelegram").value(true));
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(get("/contactos/mio"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    private static void esperarHasta(Supplier<Boolean> condicion) throws InterruptedException {
        Instant limite = Instant.now().plus(Duration.ofSeconds(5));
        while (!condicion.get()) {
            assertThat(Instant.now()).as("se esperaba que terminara el envío asincrónico").isBefore(limite);
            Thread.sleep(50);
        }
    }
}
