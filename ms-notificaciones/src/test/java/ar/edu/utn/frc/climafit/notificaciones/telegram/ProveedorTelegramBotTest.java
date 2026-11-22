package ar.edu.utn.frc.climafit.notificaciones.telegram;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba el proveedor real contra un servidor HTTP local que imita la Bot API de Telegram.
 * Lo más importante: el token del bot nunca aparece en los mensajes de error, porque esos
 * mensajes se guardan en notificacion.ultimo_error y se ven con GET /notificaciones.
 */
class ProveedorTelegramBotTest {

    private static final String TOKEN = "123456:ABC-token-secreto";

    private HttpServer servidor;
    private String url;
    private final AtomicReference<String> rutaRecibida = new AtomicReference<>();
    private final AtomicReference<String> cuerpoRecibido = new AtomicReference<>();
    private final AtomicInteger pedidos = new AtomicInteger();
    private volatile int status = 200;
    private volatile String respuesta = "{\"ok\":true,\"result\":{\"message_id\":42}}";

    @BeforeEach
    void levantar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", intercambio -> {
            pedidos.incrementAndGet();
            rutaRecibida.set(intercambio.getRequestURI().getPath());
            cuerpoRecibido.set(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(status, bytes.length);
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(bytes);
            }
        });
        servidor.start();
        url = "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    @AfterEach
    void apagar() {
        servidor.stop(0);
    }

    @Test
    void enviaElMensajeAlChatYDevuelveElMessageId() {
        String id = proveedor(url).enviar("987654321", "Tu clase de Yoga se suspendió por lluvia");

        assertThat(id).isEqualTo("42");
        assertThat(rutaRecibida.get()).isEqualTo("/bot" + TOKEN + "/sendMessage");
        assertThat(cuerpoRecibido.get())
                .contains("\"chat_id\":\"987654321\"")
                .contains("\"text\":\"Tu clase de Yoga se suspendió por lluvia\"")
                .doesNotContain("parse_mode");
    }

    @Test
    void siElUsuarioBloqueoAlBotElChatEsInaccesible() {
        status = 403;
        respuesta = "{\"ok\":false,\"error_code\":403,\"description\":\"Forbidden: bot was blocked by the user\"}";

        assertThatThrownBy(() -> proveedor(url).enviar("987654321", "hola"))
                .isInstanceOf(ChatInaccesibleException.class)
                .hasMessageContaining("403")
                .hasMessageContaining("bot was blocked by the user")
                .hasMessageNotContaining(TOKEN);
    }

    @Test
    void unChatQueNoExisteEsInaccesible() {
        status = 400;
        respuesta = "{\"ok\":false,\"error_code\":400,\"description\":\"Bad Request: chat not found\"}";

        assertThatThrownBy(() -> proveedor(url).enviar("1", "hola"))
                .isInstanceOf(ChatInaccesibleException.class)
                .hasMessageContaining("chat not found");
    }

    @Test
    void otrosErroresSeReintentanYNoDesvinculan() {
        status = 429;
        respuesta = "{\"ok\":false,\"error_code\":429,\"description\":\"Too Many Requests: retry after 5\"}";

        assertThatThrownBy(() -> proveedor(url).enviar("1", "hola"))
                .isInstanceOf(EnvioTelegramException.class)
                .isNotInstanceOf(ChatInaccesibleException.class)
                .hasMessageContaining("429");
    }

    // ---------- getMe: el usuario del bot sale del token, no de la configuración ----------

    @Test
    void elUsuarioDelBotSeLePreguntaATelegramUnaSolaVez() {
        respuesta = "{\"ok\":true,\"result\":{\"id\":1,\"is_bot\":true,\"username\":\"MiBotDePrueba\"}}";
        ProveedorTelegramBot bot = proveedor(url);

        bot.comprobarBot();

        assertThat(rutaRecibida.get()).isEqualTo("/bot" + TOKEN + "/getMe");
        assertThat(bot.usuarioBot()).contains("MiBotDePrueba");
        assertThat(bot.usuarioBot()).contains("MiBotDePrueba");
        assertThat(pedidos.get()).as("queda guardado después del primer getMe").isEqualTo(1);
    }

    @Test
    void unTokenInvalidoFrenaElArranqueSinMostrarElToken() {
        status = 401;
        respuesta = "{\"ok\":false,\"error_code\":401,\"description\":\"Unauthorized\"}";

        assertThatThrownBy(() -> proveedor(url).comprobarBot())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TELEGRAM_BOT_TOKEN no es válido")
                .hasMessageContaining("401")
                .hasMessageNotContaining(TOKEN);
    }

    @Test
    void siTelegramNoRespondeAlArrancarArrancaIgualYReintentaDespues() {
        ProveedorTelegramBot bot = proveedor(url);
        servidor.stop(0);

        bot.comprobarBot(); // no lanza

        assertThat(bot.usuarioBot()).isEmpty();
    }

    @Test
    void okFalseConStatus200TambienEsUnError() {
        respuesta = "{\"ok\":false,\"description\":\"Bad Request: chat not found\"}";

        assertThatThrownBy(() -> proveedor(url).enviar("1", "hola"))
                .isInstanceOf(EnvioTelegramException.class)
                .hasMessageContaining("chat not found");
    }

    @Test
    void sinConexionElErrorNoMuestraElToken() {
        servidor.stop(0);

        assertThatThrownBy(() -> proveedor(url).enviar("987654321", "hola"))
                .isInstanceOf(EnvioTelegramException.class)
                .hasMessageContaining("No se pudo contactar a Telegram")
                .hasMessageNotContaining(TOKEN);
    }

    @Test
    void leeLosMensajesDelBotDesdeElOffset() {
        respuesta = "{\"ok\":true,\"result\":[{\"update_id\":7,\"message\":{\"text\":\"/start ABC\"}}]}";

        var resultado = proveedor(url).obtenerActualizaciones(7);

        assertThat(resultado.isArray()).isTrue();
        assertThat(resultado.get(0).path("update_id").asLong()).isEqualTo(7);
        assertThat(rutaRecibida.get()).isEqualTo("/bot" + TOKEN + "/getUpdates");
        assertThat(cuerpoRecibido.get()).contains("\"offset\":7").contains("\"timeout\":0");
    }

    @Test
    void conflictoAlLeerMensajesNoMuestraElToken() {
        status = 409;
        respuesta = "{\"ok\":false,\"error_code\":409,\"description\":\"Conflict: can't use getUpdates method while webhook is active\"}";

        assertThatThrownBy(() -> proveedor(url).obtenerActualizaciones(0))
                .isInstanceOf(EnvioTelegramException.class)
                .hasMessageContaining("409")
                .hasMessageContaining("webhook")
                .hasMessageNotContaining(TOKEN);
    }

    @Test
    void sinTokenNoArranca() {
        assertThatThrownBy(() -> new ProveedorTelegramBot(RestClient.builder(), url, " "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TELEGRAM_BOT_TOKEN");
    }

    private ProveedorTelegramBot proveedor(String urlBase) {
        return new ProveedorTelegramBot(RestClient.builder(), urlBase, TOKEN);
    }
}
