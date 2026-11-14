package ar.edu.utn.frc.climafit.notificaciones.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;

/**
 * Envío real con la API REST de Twilio (Programmable Messaging), sin SDK: un POST con
 * autenticación básica (Account SID + Auth Token) y el cuerpo como formulario.
 * <pre>
 * POST https://api.twilio.com/2010-04-01/Accounts/{AccountSid}/Messages.json
 * From=whatsapp:+14155238886  To=whatsapp:+5493511234567  Body=...
 * </pre>
 * Twilio responde un JSON con el "sid" del mensaje, que se guarda como id del proveedor.
 */
@Component
@ConditionalOnProperty(name = "climafit.whatsapp.proveedor", havingValue = "twilio")
public class ProveedorWhatsappTwilio implements ProveedorWhatsapp {

    private static final Logger log = LoggerFactory.getLogger(ProveedorWhatsappTwilio.class);

    private final RestClient restClient;
    private final String accountSid;
    private final String from;

    public ProveedorWhatsappTwilio(RestClient.Builder builder,
                                   @Value("${climafit.whatsapp.twilio.url}") String url,
                                   @Value("${climafit.whatsapp.twilio.account-sid}") String accountSid,
                                   @Value("${climafit.whatsapp.twilio.auth-token}") String authToken,
                                   @Value("${climafit.whatsapp.twilio.from}") String from) {
        if (accountSid.isBlank() || authToken.isBlank() || from.isBlank()) {
            // Mejor fallar al arrancar que descubrirlo cuando hay que avisar una suspensión.
            throw new IllegalStateException(
                    "WHATSAPP_PROVEEDOR=twilio requiere TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN y TWILIO_FROM");
        }
        // Mismos tiempos que el resto de las llamadas HTTP del sistema: 2 s para conectar, 5 s para leer.
        SimpleClientHttpRequestFactory tiempos = new SimpleClientHttpRequestFactory();
        tiempos.setConnectTimeout(Duration.ofSeconds(2));
        tiempos.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = builder
                .baseUrl(url)
                .requestFactory(tiempos)
                .defaultHeaders(h -> h.setBasicAuth(accountSid, authToken))
                .build();
        this.accountSid = accountSid;
        this.from = from;
    }

    @Override
    public String enviar(String telefono, String mensaje) {
        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        formulario.add("From", conPrefijo(from));
        formulario.add("To", conPrefijo(telefono));
        formulario.add("Body", mensaje);
        try {
            JsonNode respuesta = restClient.post()
                    .uri("/2010-04-01/Accounts/{sid}/Messages.json", accountSid)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formulario)
                    .retrieve()
                    .body(JsonNode.class);
            String sid = respuesta != null && respuesta.hasNonNull("sid") ? respuesta.get("sid").asText() : null;
            log.info("WhatsApp enviado por Twilio a {} (sid {})", telefono, sid);
            return sid;
        } catch (RestClientResponseException ex) {
            // Twilio respondió con error (número inválido, no unido al sandbox, credenciales...)
            throw new EnvioWhatsappException("Twilio respondió " + ex.getStatusCode().value() + ": "
                    + ex.getResponseBodyAsString(), ex);
        } catch (RestClientException ex) {
            // Timeout o sin conexión
            throw new EnvioWhatsappException("No se pudo contactar a Twilio: " + ex.getMessage(), ex);
        }
    }

    /** Twilio distingue el canal por el prefijo: "whatsapp:+549..." (sin prefijo sería un SMS). */
    private static String conPrefijo(String numero) {
        return numero.startsWith("whatsapp:") ? numero : "whatsapp:" + numero;
    }
}
