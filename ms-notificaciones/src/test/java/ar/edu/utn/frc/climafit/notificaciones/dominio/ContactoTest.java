package ar.edu.utn.frc.climafit.notificaciones.dominio;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** Cambio de cuenta y desvinculación del chat de Telegram. */
class ContactoTest {

    private Contacto vinculado(String chat) {
        Contacto contacto = new Contacto("sub-alumno");
        contacto.definirConsentimiento(true);
        contacto.registrarChat(chat);
        return contacto;
    }

    @Test
    void desvinculaElChatQueYaNoAceptaMensajes() {
        Contacto contacto = vinculado("111");

        assertThat(contacto.desvincularChat("111")).isTrue();

        assertThat(contacto.tieneChat()).isFalse();
        assertThat(contacto.isAceptaTelegram()).isTrue();
        assertThat(contacto.puedeRecibirTelegram()).isFalse();
    }

    @Test
    void noDesvinculaSiMientrasTantoVinculoOtroChat() {
        Contacto contacto = vinculado("222");

        assertThat(contacto.desvincularChat("111")).isFalse();

        assertThat(contacto.getTelegramChatId()).isEqualTo("222");
    }

    @Test
    void mientrasCambiaDeCuentaSigueRecibiendoEnElChatActual() {
        Contacto contacto = vinculado("111");

        contacto.prepararVinculacion("CODIGO", LocalDateTime.now().plusHours(24));

        assertThat(contacto.puedeRecibirTelegram()).isTrue();
        assertThat(contacto.getTelegramChatId()).isEqualTo("111");
        assertThat(contacto.tieneCodigoVigente(LocalDateTime.now())).isTrue();

        contacto.registrarChat("222");

        assertThat(contacto.getTelegramChatId()).isEqualTo("222");
        assertThat(contacto.getCodigoVinculacion()).as("el código es de un solo uso").isNull();
    }
}
