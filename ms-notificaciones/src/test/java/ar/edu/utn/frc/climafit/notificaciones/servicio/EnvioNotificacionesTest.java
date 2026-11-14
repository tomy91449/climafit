package ar.edu.utn.frc.climafit.notificaciones.servicio;

import ar.edu.utn.frc.climafit.notificaciones.dominio.Contacto;
import ar.edu.utn.frc.climafit.notificaciones.dominio.EstadoNotificacion;
import ar.edu.utn.frc.climafit.notificaciones.dominio.EventoNotificacion;
import ar.edu.utn.frc.climafit.notificaciones.dominio.Notificacion;
import ar.edu.utn.frc.climafit.notificaciones.dominio.TipoEvento;
import ar.edu.utn.frc.climafit.notificaciones.repositorio.ContactoRepository;
import ar.edu.utn.frc.climafit.notificaciones.repositorio.NotificacionRepository;
import ar.edu.utn.frc.climafit.notificaciones.whatsapp.EnvioWhatsappException;
import ar.edu.utn.frc.climafit.notificaciones.whatsapp.ProveedorWhatsapp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

/** RN14 (consentimiento) y RN16 (reintentos) sin base de datos ni WhatsApp real. */
@ExtendWith(MockitoExtension.class)
class EnvioNotificacionesTest {

    private static final String ALUMNO = "sub-alumno1";
    private static final String TELEFONO = "+5493511234567";

    @Mock
    private NotificacionRepository notificaciones;
    @Mock
    private ContactoRepository contactos;
    @Mock
    private ProveedorWhatsapp proveedor;

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

    private void conContacto(String telefono, boolean acepta) {
        Contacto contacto = new Contacto(ALUMNO);
        contacto.actualizar(telefono, acepta);
        when(contactos.findById(ALUMNO)).thenReturn(Optional.of(contacto));
    }

    @Test
    void omiteSiElAlumnoNoAceptoWhatsapp() {
        conContacto(TELEFONO, false);

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
        conContacto(TELEFONO, true);
        when(proveedor.enviar(TELEFONO, "Tu clase de Yoga se suspendió por lluvia")).thenReturn("SM123");

        envio.procesar(10L);

        assertThat(notificacion.getEstado()).isEqualTo(EstadoNotificacion.ENVIADA);
        assertThat(notificacion.getIdMensajeProveedor()).isEqualTo("SM123");
        assertThat(notificacion.getTelefono()).isEqualTo(TELEFONO);
        assertThat(notificacion.getIntentos()).isEqualTo(1);
        assertThat(notificacion.getFechaEnvio()).isNotNull();
    }

    @Test
    void reintentaYSiElSegundoIntentoFuncionaQuedaEnviada() {
        conContacto(TELEFONO, true);
        when(proveedor.enviar(anyString(), anyString()))
                .thenThrow(new EnvioWhatsappException("timeout"))
                .thenReturn("SM456");

        envio.procesar(10L);

        assertThat(notificacion.getEstado()).isEqualTo(EstadoNotificacion.ENVIADA);
        assertThat(notificacion.getIntentos()).isEqualTo(2);
        assertThat(notificacion.getUltimoError()).isNull();
    }

    @Test
    void despuesDeTresFallasQuedaFallida() {
        conContacto(TELEFONO, true);
        when(proveedor.enviar(anyString(), anyString())).thenThrow(new EnvioWhatsappException("Twilio respondió 503"));

        envio.procesar(10L);

        assertThat(notificacion.getEstado()).isEqualTo(EstadoNotificacion.FALLIDA);
        assertThat(notificacion.getIntentos()).isEqualTo(3);
        assertThat(notificacion.getUltimoError()).contains("503");
        verify(proveedor, times(3)).enviar(anyString(), anyString());
    }

    @Test
    void noVuelveAProcesarUnaNotificacionQueYaNoEstaPendiente() {
        notificacion.omitir("ya procesada");

        envio.procesar(10L);

        verify(contactos, never()).findById(anyString());
        verify(proveedor, never()).enviar(anyString(), anyString());
    }
}
