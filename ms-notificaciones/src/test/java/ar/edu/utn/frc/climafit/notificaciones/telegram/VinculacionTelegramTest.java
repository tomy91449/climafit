package ar.edu.utn.frc.climafit.notificaciones.telegram;

import ar.edu.utn.frc.climafit.notificaciones.servicio.ContactoService;
import ar.edu.utn.frc.climafit.notificaciones.servicio.ContactoService.ResultadoVinculacion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lectura de los "/start" que le llegan al bot y respuesta al usuario. */
class VinculacionTelegramTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private ProveedorTelegramBot bot;
    private ContactoService contactos;
    private VinculacionTelegram vinculacion;

    @BeforeEach
    void preparar() {
        bot = mock(ProveedorTelegramBot.class);
        contactos = mock(ContactoService.class);
        vinculacion = new VinculacionTelegram(bot, contactos);
    }

    private static JsonNode actualizaciones(String json) throws Exception {
        return JSON.readTree(json);
    }

    private static String mensaje(long updateId, String chatId, String tipoChat, String texto) {
        return """
                {"update_id": %d, "message": {"message_id": 1, "chat": {"id": %s, "type": "%s"}, "text": "%s"}}
                """.formatted(updateId, chatId, tipoChat, texto);
    }

    @Test
    void startConCodigoValidoVinculaYConfirma() throws Exception {
        when(bot.obtenerActualizaciones(0)).thenReturn(actualizaciones(
                "[" + mensaje(10, "8246838480", "private", "/start ABC123") + "]"));
        when(contactos.vincular("ABC123", "8246838480")).thenReturn(ResultadoVinculacion.VINCULADO);

        vinculacion.revisarMensajes();

        verify(contactos).vincular("ABC123", "8246838480");
        verify(bot).enviar("8246838480", VinculacionTelegram.MENSAJE_VINCULADO);
    }

    @Test
    void codigoInvalidoAvisaQueHayQueGenerarOtro() throws Exception {
        when(bot.obtenerActualizaciones(0)).thenReturn(actualizaciones(
                "[" + mensaje(10, "1", "private", "/start VENCIDO") + "]"));
        when(contactos.vincular("VENCIDO", "1")).thenReturn(ResultadoVinculacion.CODIGO_INVALIDO);

        vinculacion.revisarMensajes();

        verify(bot).enviar("1", VinculacionTelegram.MENSAJE_CODIGO_INVALIDO);
    }

    @Test
    void startSinCodigoExplicaComoVincular() throws Exception {
        when(bot.obtenerActualizaciones(0)).thenReturn(actualizaciones(
                "[" + mensaje(10, "1", "private", "/start") + "]"));

        vinculacion.revisarMensajes();

        verify(contactos, never()).vincular(anyString(), anyString());
        verify(bot).enviar("1", VinculacionTelegram.MENSAJE_SIN_CODIGO);
    }

    @Test
    void otrosMensajesYGruposSeIgnoran() throws Exception {
        when(bot.obtenerActualizaciones(0)).thenReturn(actualizaciones("["
                + mensaje(10, "1", "private", "Hola") + ","
                + mensaje(11, "-100", "group", "/start ABC123") + ","
                + mensaje(12, "1", "private", "/startABC") + "]"));

        vinculacion.revisarMensajes();

        verify(contactos, never()).vincular(anyString(), anyString());
        verify(bot, never()).enviar(anyString(), anyString());
    }

    @Test
    void confirmaLosMensajesLeidosPidiendoDesdeElSiguiente() throws Exception {
        when(bot.obtenerActualizaciones(0)).thenReturn(actualizaciones(
                "[" + mensaje(41, "1", "private", "Hola") + "," + mensaje(42, "1", "private", "Chau") + "]"));
        when(bot.obtenerActualizaciones(43)).thenReturn(actualizaciones("[]"));

        vinculacion.revisarMensajes();
        vinculacion.revisarMensajes();

        verify(bot).obtenerActualizaciones(43);
    }

    @Test
    void siFallaLaBaseNoConfirmaElMensajeYLoReintenta() throws Exception {
        when(bot.obtenerActualizaciones(0)).thenReturn(actualizaciones(
                "[" + mensaje(41, "1", "private", "/start ABC") + "]"));
        when(contactos.vincular("ABC", "1")).thenThrow(new IllegalStateException("base caída"));

        vinculacion.revisarMensajes();
        vinculacion.revisarMensajes();

        // Sigue pidiendo desde 0: el mensaje 41 no se confirmó.
        verify(bot, times(2)).obtenerActualizaciones(0);
        verify(bot, never()).obtenerActualizaciones(42);
    }

    @Test
    void siTelegramNoRespondeNoRompe() {
        when(bot.obtenerActualizaciones(anyLong())).thenThrow(new EnvioTelegramException("sin conexión"));

        vinculacion.revisarMensajes();

        verify(contactos, never()).vincular(anyString(), anyString());
    }

    @Test
    void siNoPuedeResponderLaVinculacionIgualQuedaHecha() throws Exception {
        when(bot.obtenerActualizaciones(0)).thenReturn(actualizaciones(
                "[" + mensaje(10, "1", "private", "/start ABC") + "]"));
        when(contactos.vincular("ABC", "1")).thenReturn(ResultadoVinculacion.VINCULADO);
        doThrow(new EnvioTelegramException("403")).when(bot).enviar(anyString(), anyString());
        when(bot.obtenerActualizaciones(11)).thenReturn(actualizaciones("[]"));

        vinculacion.revisarMensajes();
        vinculacion.revisarMensajes();

        verify(contactos).vincular("ABC", "1");
        verify(bot).obtenerActualizaciones(11);
    }
}
