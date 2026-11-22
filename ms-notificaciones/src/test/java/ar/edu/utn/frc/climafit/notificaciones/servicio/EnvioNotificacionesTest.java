package ar.edu.utn.frc.climafit.notificaciones.servicio;

import ar.edu.utn.frc.climafit.notificaciones.dominio.Contacto;
import ar.edu.utn.frc.climafit.notificaciones.dominio.EstadoNotificacion;
import ar.edu.utn.frc.climafit.notificaciones.dominio.EventoNotificacion;
import ar.edu.utn.frc.climafit.notificaciones.dominio.Notificacion;
import ar.edu.utn.frc.climafit.notificaciones.dominio.TipoEvento;
import ar.edu.utn.frc.climafit.notificaciones.repositorio.ContactoRepository;
import ar.edu.utn.frc.climafit.notificaciones.repositorio.NotificacionRepository;
import ar.edu.utn.frc.climafit.notificaciones.telegram.ChatInaccesibleException;
import ar.edu.utn.frc.climafit.notificaciones.telegram.EnvioTelegramException;
import ar.edu.utn.frc.climafit.notificaciones.telegram.ProveedorTelegram;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** RN14 (consentimiento) y RN16 (reintentos) sin base de datos ni Telegram real. */
@ExtendWith(MockitoExtension.class)
class EnvioNotificacionesTest {

    private static final String ALUMNO = "sub-alumno1";
    private static final String CHAT = "123456789";

    @Mock
    private NotificacionRepository notificaciones;
    @Mock
    private ContactoRepository contactos;
    @Mock
    private ProveedorTelegram proveedor;

    private EnvioNotificaciones envio;
    private Notificacion notificacion;

    @BeforeEach
    void preparar() {
        // Espera 0 para que el test de reintentos no tarde.
        envio = new EnvioNotificaciones(notificaciones, contactos, proveedor, 3, Duration.ZERO);
        var evento = new EventoNotificacion("clase-1-cambio-1", TipoEvento.SUSPENSION, 1L, "sub-profe1",
                "Tu clase de Yoga se suspendió por lluvia");
        notificacion = new Notificacion(evento, ALUMNO);
        when(notificaciones.findConEvento(10L)).thenReturn(Optional.of(notificacion));
    }

    private void conContacto(String chatId, boolean acepta) {
        Contacto contacto = new Contacto(ALUMNO);
        contacto.definirConsentimiento(acepta);
        contacto.registrarChat(chatId);
        when(contactos.findById(ALUMNO)).thenReturn(Optional.of(contacto));
    }

    @Test
    void omiteSiElAlumnoNoAceptoTelegram() {
        conContacto(CHAT, false);

        envio.procesar(10L);

        assertThat(notificacion.getEstado()).isEqualTo(EstadoNotificacion.OMITIDA);
        assertThat(notificacion.getIntentos()).isZero();
        verify(proveedor, never()).enviar(anyString(), anyString());
    }

    @Test
    void omiteSiElUsuarioNuncaRegistroSuContacto() {
        when(contactos.findById(ALUMNO)).thenReturn(Optional.empty());

        envio.procesar(10L);

        assertThat(notificacion.getEstado()).isEqualTo(EstadoNotificacion.OMITIDA);
        verify(proveedor, never()).enviar(anyString(), anyString());
    }

    @Test
    void enviaYGuardaElIdDelProveedor() {
        conContacto(CHAT, true);
        when(proveedor.enviar(CHAT, "Tu clase de Yoga se suspendió por lluvia")).thenReturn("42");

        envio.procesar(10L);

        assertThat(notificacion.getEstado()).isEqualTo(EstadoNotificacion.ENVIADA);
        assertThat(notificacion.getIdMensajeProveedor()).isEqualTo("42");
        assertThat(notificacion.getTelegramChatId()).isEqualTo(CHAT);
        assertThat(notificacion.getIntentos()).isEqualTo(1);
        assertThat(notificacion.getFechaEnvio()).isNotNull();
    }

    @Test
    void reintentaYSiElSegundoIntentoFuncionaQuedaEnviada() {
        conContacto(CHAT, true);
        when(proveedor.enviar(anyString(), anyString()))
                .thenThrow(new EnvioTelegramException("timeout"))
                .thenReturn("43");

        envio.procesar(10L);

        assertThat(notificacion.getEstado()).isEqualTo(EstadoNotificacion.ENVIADA);
        assertThat(notificacion.getIntentos()).isEqualTo(2);
        assertThat(notificacion.getUltimoError()).isNull();
    }

    @Test
    void despuesDeTresFallasQuedaFallida() {
        conContacto(CHAT, true);
        when(proveedor.enviar(anyString(), anyString())).thenThrow(new EnvioTelegramException("Telegram respondió 503"));

        envio.procesar(10L);

        assertThat(notificacion.getEstado()).isEqualTo(EstadoNotificacion.FALLIDA);
        assertThat(notificacion.getIntentos()).isEqualTo(3);
        assertThat(notificacion.getUltimoError()).contains("503");
        verify(proveedor, times(3)).enviar(anyString(), anyString());
    }

    @Test
    void siElUsuarioBloqueoAlBotFallaSinReintentarYSeDesvinculaElChat() {
        conContacto(CHAT, true);
        when(proveedor.enviar(anyString(), anyString()))
                .thenThrow(new ChatInaccesibleException("Telegram respondió 403: Forbidden: bot was blocked by the user"));

        envio.procesar(10L);

        assertThat(notificacion.getEstado()).isEqualTo(EstadoNotificacion.FALLIDA);
        assertThat(notificacion.getIntentos()).isEqualTo(1);
        assertThat(notificacion.getUltimoError()).contains("blocked");
        verify(proveedor, times(1)).enviar(anyString(), anyString());

        ArgumentCaptor<Contacto> guardado = ArgumentCaptor.forClass(Contacto.class);
        verify(contactos).save(guardado.capture());
        assertThat(guardado.getValue().tieneChat()).isFalse();
        assertThat(guardado.getValue().isAceptaTelegram()).as("el consentimiento no cambia").isTrue();
    }

    @Test
    void noVuelveAProcesarUnaNotificacionQueYaNoEstaPendiente() {
        notificacion.omitir("ya procesada");

        envio.procesar(10L);

        verify(contactos, never()).findById(anyString());
        verify(proveedor, never()).enviar(anyString(), anyString());
    }
}
