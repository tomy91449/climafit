package ar.edu.utn.frc.climafit.notificaciones.telegram;

import ar.edu.utn.frc.climafit.notificaciones.servicio.ContactoService;
import ar.edu.utn.frc.climafit.notificaciones.servicio.ContactoService.ResultadoVinculacion;
import ar.edu.utn.frc.climafit.notificaciones.servicio.ContactoService.Vinculacion;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Vinculación automática: lee los mensajes que le llegan al bot y, cuando alguien toca "Iniciar"
 * desde el link {@code t.me/<bot>?start=<código>}, guarda su chat en el contacto que generó ese
 * código. Así el usuario no tiene que averiguar ni cargar su chat_id.
 * <p>
 * Usa getUpdates cada pocos segundos (polling) en lugar de un webhook: no necesita una URL pública
 * con HTTPS y funciona igual desde el IDE o desde Docker. Solo corre con el bot real
 * ({@code TELEGRAM_PROVEEDOR=bot}); con el proveedor "log" el chat se carga a mano.
 */
@Component
@ConditionalOnProperty(name = "climafit.telegram.proveedor", havingValue = "bot")
public class VinculacionTelegram {

    private static final Logger log = LoggerFactory.getLogger(VinculacionTelegram.class);

    static final String MENSAJE_VINCULADO = "¡Listo! Tu Telegram quedó vinculado a ClimaFit. "
            + "Por acá te vamos a avisar los cambios de tus clases.";
    static final String MENSAJE_CODIGO_INVALIDO = "Este link no es válido o ya venció. "
            + "Generá uno nuevo desde ClimaFit, en tus preferencias de contacto.";
    static final String MENSAJE_SIN_CODIGO = "Hola, soy el bot de avisos de ClimaFit. "
            + "Para recibir avisos, aceptalos desde ClimaFit y abrí el link que te damos ahí.";
    static final String MENSAJE_CUENTA_CAMBIADA = "Tu cuenta de ClimaFit se vinculó a otro Telegram. "
            + "Desde ahora los avisos llegan allá y no a este chat.";

    private final ProveedorTelegramBot bot;
    private final ContactoService contactos;

    /** Próximo update_id a pedir. Pedir desde acá le confirma a Telegram los anteriores. */
    private long offset = 0;
    /** Para no llenar el log con el mismo error cada 3 segundos si Telegram no responde. */
    private boolean veniaFallando = false;

    public VinculacionTelegram(ProveedorTelegramBot bot, ContactoService contactos) {
        this.bot = bot;
        this.contactos = contactos;
    }

    @Scheduled(fixedDelayString = "${climafit.telegram.vinculacion.cada:PT3S}")
    public void revisarMensajes() {
        JsonNode actualizaciones;
        try {
            actualizaciones = bot.obtenerActualizaciones(offset);
        } catch (EnvioTelegramException ex) {
            if (!veniaFallando) {
                log.warn("No se pudieron leer los mensajes del bot: {}", ex.getMessage());
                veniaFallando = true;
            }
            return;
        }
        if (veniaFallando) {
            log.info("Se volvieron a leer los mensajes del bot");
            veniaFallando = false;
        }

        for (JsonNode actualizacion : actualizaciones) {
            try {
                procesar(actualizacion.path("message"));
            } catch (RuntimeException ex) {
                // Por ejemplo, la base no respondió. No se confirma este mensaje: se reintenta en la próxima vuelta.
                log.warn("No se pudo procesar un mensaje del bot: {}", ex.getMessage());
                return;
            }
            offset = Math.max(offset, actualizacion.path("update_id").asLong() + 1);
        }
    }

    /** Solo interesan los "/start" de chats privados; el resto de los mensajes se ignora. */
    void procesar(JsonNode mensaje) {
        if (mensaje.isMissingNode() || !"private".equals(mensaje.path("chat").path("type").asText())) {
            return;
        }
        String texto = mensaje.path("text").asText("").trim();
        if (!texto.equals("/start") && !texto.startsWith("/start ")) {
            return;
        }
        String chatId = mensaje.path("chat").path("id").asText();
        String codigo = texto.substring("/start".length()).trim();

        if (codigo.isEmpty()) {
            responder(chatId, MENSAJE_SIN_CODIGO);
            return;
        }
        Vinculacion vinculacion = contactos.vincular(codigo, chatId);
        if (vinculacion.resultado() != ResultadoVinculacion.VINCULADO) {
            responder(chatId, MENSAJE_CODIGO_INVALIDO);
            return;
        }
        responder(chatId, MENSAJE_VINCULADO);
        if (vinculacion.cambioDeCuenta(chatId)) {
            // Cambio de cuenta: el chat viejo deja de recibir avisos, así que se le cuenta por qué.
            responder(vinculacion.chatAnterior(), MENSAJE_CUENTA_CAMBIADA);
        }
    }

    /** La respuesta es solo informativa: si falla, la vinculación igual quedó hecha. */
    private void responder(String chatId, String texto) {
        try {
            bot.enviar(chatId, texto);
        } catch (EnvioTelegramException ex) {
            log.warn("No se pudo responder al chat {}: {}", chatId, ex.getMessage());
        }
    }
}
