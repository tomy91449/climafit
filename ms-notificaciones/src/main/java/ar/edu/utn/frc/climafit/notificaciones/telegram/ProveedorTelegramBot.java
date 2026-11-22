package ar.edu.utn.frc.climafit.notificaciones.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Envío real con la Bot API de Telegram, sin SDK: un POST con JSON.
 * <pre>
 * POST https://api.telegram.org/bot{TOKEN}/sendMessage
 * {"chat_id": "123456789", "text": "..."}
 * -> {"ok": true, "result": {"message_id": 42, ...}}
 * </pre>
 * El message_id se guarda como id del proveedor. El texto va sin {@code parse_mode}: se manda tal
 * cual, sin interpretar Markdown ni HTML.
 * <p>
 * El usuario del bot (para el link de vinculación) no se configura: se le pregunta a Telegram con
 * {@code getMe}. Así el link siempre lleva al bot dueño del token y nunca al de otra persona.
 * <p>
 * <b>El token del bot es un secreto y viaja en la URL.</b> Los errores de {@link RestClient} incluyen
 * la URL en su mensaje, y ese mensaje termina en {@code notificacion.ultimo_error}, que se puede ver
 * con GET /notificaciones. Por eso todo mensaje de error pasa por {@link #sinToken(String)}.
 */
@Component
@ConditionalOnProperty(name = "climafit.telegram.proveedor", havingValue = "bot")
public class ProveedorTelegramBot implements ProveedorTelegram {

    private static final Logger log = LoggerFactory.getLogger(ProveedorTelegramBot.class);
    private static final String TOKEN_OCULTO = "<token>";

    private final RestClient restClient;
    private final String token;

    /** Usuario del bot según getMe. Null mientras Telegram no haya respondido. */
    private volatile String usuario;

    public ProveedorTelegramBot(RestClient.Builder builder,
                                @Value("${climafit.telegram.bot.url}") String url,
                                @Value("${climafit.telegram.bot.token}") String token) {
        if (token == null || token.isBlank()) {
            // Mejor fallar al arrancar que descubrirlo cuando hay que avisar una suspensión.
            throw new IllegalStateException("TELEGRAM_PROVEEDOR=bot requiere TELEGRAM_BOT_TOKEN");
        }
        // Mismos tiempos que el resto de las llamadas HTTP del sistema: 2 s para conectar, 5 s para leer.
        SimpleClientHttpRequestFactory tiempos = new SimpleClientHttpRequestFactory();
        tiempos.setConnectTimeout(Duration.ofSeconds(2));
        tiempos.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = builder.clone()
                .baseUrl(url)
                .requestFactory(tiempos)
                .build();
        this.token = token;
    }

    /**
     * Al arrancar se confirma el token con getMe. Un token inválido frena el arranque con un mensaje
     * claro. Si Telegram no responde (sin Internet, por ejemplo), el servicio arranca igual y vuelve
     * a preguntar la primera vez que haga falta armar un link.
     */
    @PostConstruct
    void comprobarBot() {
        try {
            usuario = consultarUsuario();
            log.info("Bot de Telegram listo: @{}", usuario);
        } catch (EnvioTelegramException ex) {
            log.warn("No se pudo consultar el bot de Telegram al arrancar; se reintenta al pedir un link: {}",
                    ex.getMessage());
        }
    }

    @Override
    public Optional<String> usuarioBot() {
        if (usuario == null) {
            try {
                usuario = consultarUsuario();
            } catch (RuntimeException ex) {
                log.warn("No se pudo consultar el bot de Telegram: {}", ex.getMessage());
            }
        }
        return Optional.ofNullable(usuario);
    }

    @Override
    public String enviar(String chatId, String mensaje) {
        JsonNode respuesta;
        try {
            respuesta = restClient.post()
                    .uri("/bot{token}/sendMessage", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("chat_id", chatId, "text", mensaje))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            String texto = sinToken("Telegram respondió " + status + ": " + descripcion(ex));
            if (chatInaccesible(status, descripcion(ex))) {
                // 403: el usuario bloqueó al bot. 400 "chat not found": el chat ya no existe.
                throw new ChatInaccesibleException(texto);
            }
            // Token inválido (401), demasiados pedidos (429), Telegram caído (5xx)...
            throw new EnvioTelegramException(texto);
        } catch (RestClientException ex) {
            // Timeout o sin conexión. Sin la causa: su mensaje trae la URL con el token.
            throw new EnvioTelegramException(sinToken("No se pudo contactar a Telegram: " + ex.getMessage()));
        }

        JsonNode resultado = exigirOk(respuesta, "Telegram no confirmó el envío");
        String id = resultado.path("message_id").asText(null);
        log.info("Telegram enviado al chat {} (message_id {})", chatId, id);
        return id;
    }

    /**
     * Mensajes que le llegaron al bot desde {@code offset} (getUpdates sin espera). Pasar como offset
     * el último update_id + 1 le confirma a Telegram los anteriores, que ya no se vuelven a recibir.
     *
     * @return el arreglo "result" de la respuesta (vacío si no hay mensajes nuevos)
     * @throws EnvioTelegramException si Telegram respondió con error o no respondió
     */
    public JsonNode obtenerActualizaciones(long offset) {
        JsonNode respuesta;
        try {
            respuesta = restClient.post()
                    .uri("/bot{token}/getUpdates", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("offset", offset, "timeout", 0, "allowed_updates", new String[]{"message"}))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException ex) {
            // 409: el bot tiene un webhook configurado y Telegram no deja usar getUpdates a la vez.
            throw new EnvioTelegramException(sinToken("Telegram respondió " + ex.getStatusCode().value() + ": "
                    + descripcion(ex)));
        } catch (RestClientException ex) {
            throw new EnvioTelegramException(sinToken("No se pudo contactar a Telegram: " + ex.getMessage()));
        }
        return exigirOk(respuesta, "Telegram no devolvió los mensajes");
    }

    /**
     * Usuario del bot dueño del token (getMe).
     *
     * @throws IllegalStateException  si Telegram rechaza el token (401 o 404): es un error de configuración
     * @throws EnvioTelegramException si Telegram no respondió o respondió otro error
     */
    String consultarUsuario() {
        JsonNode respuesta;
        try {
            respuesta = restClient.get()
                    .uri("/bot{token}/getMe", token)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            String texto = sinToken("Telegram respondió " + status + ": " + descripcion(ex));
            if (status == 401 || status == 404) {
                throw new IllegalStateException("TELEGRAM_BOT_TOKEN no es válido. " + texto);
            }
            throw new EnvioTelegramException(texto);
        } catch (RestClientException ex) {
            throw new EnvioTelegramException(sinToken("No se pudo contactar a Telegram: " + ex.getMessage()));
        }
        String nombre = exigirOk(respuesta, "Telegram no devolvió los datos del bot").path("username").asText("");
        if (nombre.isBlank()) {
            throw new EnvioTelegramException("Telegram no devolvió el usuario del bot");
        }
        return nombre;
    }

    /** Devuelve el campo "result" si la respuesta dice ok: true; si no, un error con la descripción. */
    private JsonNode exigirOk(JsonNode respuesta, String contexto) {
        if (respuesta == null || !respuesta.path("ok").asBoolean(false)) {
            String detalle = respuesta == null ? "respuesta vacía" : respuesta.path("description").asText("sin detalle");
            throw new EnvioTelegramException(sinToken(contexto + ": " + detalle));
        }
        return respuesta.path("result");
    }

    private static boolean chatInaccesible(int status, String descripcion) {
        return status == 403 || (status == 400 && descripcion != null && descripcion.contains("chat not found"));
    }

    /** Telegram explica el error en el campo "description" del cuerpo. */
    private static String descripcion(RestClientResponseException ex) {
        try {
            JsonNode cuerpo = ex.getResponseBodyAs(JsonNode.class);
            if (cuerpo != null && cuerpo.hasNonNull("description")) {
                return cuerpo.get("description").asText();
            }
        } catch (RuntimeException ignorada) {
            // el cuerpo no era JSON: se usa tal cual
        }
        return ex.getResponseBodyAsString();
    }

    private String sinToken(String texto) {
        return texto == null ? null : texto.replace(token, TOKEN_OCULTO);
    }
}
