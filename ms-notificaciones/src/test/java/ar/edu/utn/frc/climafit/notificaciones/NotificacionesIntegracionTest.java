package ar.edu.utn.frc.climafit.notificaciones;

import ar.edu.utn.frc.climafit.notificaciones.dominio.EstadoNotificacion;
import ar.edu.utn.frc.climafit.notificaciones.repositorio.NotificacionRepository;
import ar.edu.utn.frc.climafit.notificaciones.servicio.ContactoService;
import ar.edu.utn.frc.climafit.notificaciones.servicio.ContactoService.ResultadoVinculacion;
import ar.edu.utn.frc.climafit.notificaciones.telegram.ChatInaccesibleException;
import ar.edu.utn.frc.climafit.notificaciones.telegram.ProveedorTelegram;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
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
 * <p>
 * El proveedor es el de "log" (sin bot real), envuelto en un spy: los tests de vinculación le hacen
 * devolver un usuario de bot, como si hubiera respondido getMe.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificacionesIntegracionTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private NotificacionRepository notificaciones;
    @Autowired
    private ContactoService contactos;
    @MockitoSpyBean
    private ProveedorTelegram proveedor;

    private static final String BOT = "ClimaFitPruebaBot";

    /** Simula un bot real: getMe respondió con este usuario. */
    private void conBot() {
        doReturn(Optional.of(BOT)).when(proveedor).usuarioBot();
    }

    private static String codigoDe(String link) {
        return link.substring(link.indexOf("start=") + "start=".length());
    }

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
    void aceptarSinChatDevuelveElLinkYAlTocarIniciarQuedaVinculado() throws Exception {
        conBot();
        String respuesta = mvc.perform(put("/contactos/mio").with(usuario("alu-y", "ALUMNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aceptaTelegram\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vinculado").value(false))
                .andExpect(jsonPath("$.telegramChatId").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String link = JsonPath.read(respuesta, "$.linkVinculacion");
        assertThat(link).startsWith("https://t.me/" + BOT + "?start=");
        String codigo = link.substring(link.indexOf("start=") + "start=".length());
        assertThat(codigo).matches("[A-Za-z0-9_-]{32}");

        // Guardar de nuevo sin vincular no cambia el link.
        mvc.perform(put("/contactos/mio").with(usuario("alu-y", "ALUMNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aceptaTelegram\": true}"))
                .andExpect(jsonPath("$.linkVinculacion").value(link));

        // El usuario toca "Iniciar": el bot recibe "/start <código>" desde su chat.
        assertThat(contactos.vincular(codigo, "555000111").resultado()).isEqualTo(ResultadoVinculacion.VINCULADO);

        mvc.perform(get("/contactos/mio").with(usuario("alu-y", "ALUMNO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vinculado").value(true))
                .andExpect(jsonPath("$.telegramChatId").value("555000111"))
                .andExpect(jsonPath("$.linkVinculacion").doesNotExist());

        // El código es de un solo uso.
        assertThat(contactos.vincular(codigo, "999").resultado()).isEqualTo(ResultadoVinculacion.CODIGO_INVALIDO);
    }

    @Test
    void rechazarLosAvisosAnulaElLinkPendiente() throws Exception {
        conBot();
        String respuesta = mvc.perform(put("/contactos/mio").with(usuario("alu-z", "ALUMNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aceptaTelegram\": true}"))
                .andReturn().getResponse().getContentAsString();
        String link = JsonPath.read(respuesta, "$.linkVinculacion");
        String codigo = link.substring(link.indexOf("start=") + "start=".length());

        mvc.perform(put("/contactos/mio").with(usuario("alu-z", "ALUMNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aceptaTelegram\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkVinculacion").doesNotExist());

        assertThat(contactos.vincular(codigo, "555000222").resultado()).isEqualTo(ResultadoVinculacion.CODIGO_INVALIDO);
        assertThat(contactos.vincular("codigo-que-no-existe", "555000222").resultado())
                .isEqualTo(ResultadoVinculacion.CODIGO_INVALIDO);
    }

    @Test
    void sinBotRealNoHayLinkYElChatSeCargaAMano() throws Exception {
        // Proveedor "log": getMe no existe, así que no se genera ningún link.
        mvc.perform(put("/contactos/mio").with(usuario("alu-demo", "ALUMNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aceptaTelegram\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vinculado").value(false))
                .andExpect(jsonPath("$.linkVinculacion").doesNotExist());

        mvc.perform(post("/contactos/mio/vinculacion").with(usuario("alu-demo", "ALUMNO")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("SERVICIO_NO_DISPONIBLE"));
    }

    @Test
    void cambiarDeCuentaDeTelegramConservaElChatHastaElNuevoStart() throws Exception {
        conBot();
        // Ya tiene un Telegram vinculado.
        mvc.perform(put("/contactos/mio").with(usuario("alu-cambia", "ALUMNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telegramChatId\": \"111000111\", \"aceptaTelegram\": true}"))
                .andExpect(jsonPath("$.vinculado").value(true))
                .andExpect(jsonPath("$.linkVinculacion").doesNotExist());

        // Pide un link para pasar a otra cuenta: el chat actual se mantiene mientras tanto.
        String primero = mvc.perform(post("/contactos/mio/vinculacion").with(usuario("alu-cambia", "ALUMNO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vinculado").value(true))
                .andExpect(jsonPath("$.telegramChatId").value("111000111"))
                .andExpect(jsonPath("$.linkVence").exists())
                .andReturn().getResponse().getContentAsString();
        String linkViejo = JsonPath.read(primero, "$.linkVinculacion");
        assertThat(linkViejo).startsWith("https://t.me/" + BOT + "?start=");

        // Pedir otro link invalida el anterior.
        String segundo = mvc.perform(post("/contactos/mio/vinculacion").with(usuario("alu-cambia", "ALUMNO")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String linkNuevo = JsonPath.read(segundo, "$.linkVinculacion");
        assertThat(linkNuevo).isNotEqualTo(linkViejo);
        assertThat(contactos.vincular(codigoDe(linkViejo), "222000222").resultado())
                .isEqualTo(ResultadoVinculacion.CODIGO_INVALIDO);

        // Toca Iniciar desde la cuenta nueva.
        var vinculacion = contactos.vincular(codigoDe(linkNuevo), "222000222");
        assertThat(vinculacion.resultado()).isEqualTo(ResultadoVinculacion.VINCULADO);
        assertThat(vinculacion.chatAnterior()).isEqualTo("111000111");
        assertThat(vinculacion.cambioDeCuenta("222000222")).isTrue();

        mvc.perform(get("/contactos/mio").with(usuario("alu-cambia", "ALUMNO")))
                .andExpect(jsonPath("$.vinculado").value(true))
                .andExpect(jsonPath("$.telegramChatId").value("222000222"))
                .andExpect(jsonPath("$.linkVinculacion").doesNotExist());
    }

    @Test
    void pedirLinkSinPreferenciasOSinAceptarAvisos() throws Exception {
        conBot();
        mvc.perform(post("/contactos/mio/vinculacion").with(usuario("alu-sin-contacto", "ALUMNO")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));

        mvc.perform(put("/contactos/mio").with(usuario("alu-no-acepta", "ALUMNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aceptaTelegram\": false}"))
                .andExpect(status().isOk());
        mvc.perform(post("/contactos/mio/vinculacion").with(usuario("alu-no-acepta", "ALUMNO")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("AVISOS_NO_ACEPTADOS"));
    }

    @Test
    void siElUsuarioBloqueoAlBotSeDesvinculaYRecibeUnLinkNuevo() throws Exception {
        conBot();
        mvc.perform(put("/contactos/mio").with(usuario("alu-bloquea", "ALUMNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telegramChatId\": \"333000333\", \"aceptaTelegram\": true}"))
                .andExpect(status().isOk());
        doThrow(new ChatInaccesibleException("Telegram respondió 403: Forbidden: bot was blocked by the user"))
                .when(proveedor).enviar(eq("333000333"), anyString());

        mvc.perform(post("/internal/notificaciones").with(SERVICIO).contentType(MediaType.APPLICATION_JSON)
                        .content(evento("clase-300-cambio-1", 300, "profe-a", "\"alu-bloquea\"")))
                .andExpect(status().isAccepted());
        esperarHasta(() -> notificaciones.findByClase(300L).stream().noneMatch(n -> n.estaPendiente()));

        // Falla en el primer intento, sin reintentar, y el chat se desvincula.
        assertThat(notificaciones.findByClase(300L)).singleElement().satisfies(n -> {
            assertThat(n.getEstado()).isEqualTo(EstadoNotificacion.FALLIDA);
            assertThat(n.getIntentos()).isEqualTo(1);
            assertThat(n.getUltimoError()).contains("blocked");
        });
        mvc.perform(get("/contactos/mio").with(usuario("alu-bloquea", "ALUMNO")))
                .andExpect(jsonPath("$.vinculado").value(false))
                .andExpect(jsonPath("$.aceptaTelegram").value(true));

        // Vuelve a aceptar los avisos: recibe un link para vincular de nuevo.
        mvc.perform(put("/contactos/mio").with(usuario("alu-bloquea", "ALUMNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aceptaTelegram\": true}"))
                .andExpect(jsonPath("$.linkVinculacion").exists());
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
