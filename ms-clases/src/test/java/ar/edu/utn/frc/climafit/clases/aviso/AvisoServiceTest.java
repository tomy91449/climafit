package ar.edu.utn.frc.climafit.clases.aviso;

import ar.edu.utn.frc.climafit.clases.BaseIntegracionTest;
import ar.edu.utn.frc.climafit.clases.clase.Clase;
import ar.edu.utn.frc.climafit.clases.estado.CambioEstado;
import ar.edu.utn.frc.climafit.clases.estado.CambioEstadoService;
import ar.edu.utn.frc.climafit.clases.estado.EstadoClase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AvisoServiceTest extends BaseIntegracionTest {

    @Autowired AvisoService avisos;
    @Autowired CambioEstadoService estados;

    private CambioEstado suspender(Clase clase) {
        return estados.cambiarEstado(clase, EstadoClase.SUSPENDIDA, "Probabilidad de lluvia 90 % (máximo 20 %)", "SISTEMA");
    }

    @Test
    void elEventoIdTieneElFormatoClaseNCambioN() {
        Clase clase = claseEnBase(PROFE_1, manana(18), 60, 5, EstadoClase.PROGRAMADA);
        CambioEstado cambio = suspender(clase);

        assertThat(avisos.eventoId(cambio)).isEqualTo("CLASE-" + clase.getId() + "-CAMBIO-" + cambio.getId());
    }

    @Test
    void avisarMarcaNotificadoCuandoMsNotificacionesResponde() {
        Clase clase = claseEnBase(PROFE_1, manana(18), 60, 5, EstadoClase.PROGRAMADA);
        CambioEstado cambio = suspender(clase);

        avisos.avisar(cambio, TipoEvento.SUSPENSION, null, List.of(ALUMNO_1, ALUMNO_2));

        assertThat(cambios.findById(cambio.getId()).orElseThrow().isNotificado()).isTrue();
        assertThat(notificaciones.eventos()).hasSize(1);
        EventoNotificacion evento = notificaciones.eventos().get(0);
        assertThat(evento.eventoId()).isEqualTo(avisos.eventoId(cambio));
        assertThat(evento.tipo()).isEqualTo(TipoEvento.SUSPENSION);
        assertThat(evento.claseId()).isEqualTo(clase.getId());
        assertThat(evento.profesorId()).isEqualTo(PROFE_1);
        assertThat(evento.destinatarios()).containsExactly(ALUMNO_1, ALUMNO_2);
    }

    @Test
    void elMensajeEstaEnEspanolConActividadFechaYMotivo() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(1).withHour(18).withMinute(0).withSecond(0).withNano(0);
        Clase clase = claseEnBase(PROFE_1, inicio, 60, 5, EstadoClase.PROGRAMADA); // actividad 1 = Yoga
        CambioEstado cambio = suspender(clase);

        avisos.avisar(cambio, TipoEvento.SUSPENSION, null, List.of(ALUMNO_1));

        String mensaje = notificaciones.eventos().get(0).mensaje();
        assertThat(mensaje).startsWith("Tu clase de Yoga del ")
                .contains(String.format("%02d/%02d 18:00", inicio.getDayOfMonth(), inicio.getMonthValue()))
                .contains("se suspendió")
                .contains("Probabilidad de lluvia 90 % (máximo 20 %)")
                .endsWith(".");
    }

    @Test
    void elMensajeDeUnaReprogramacionIncluyeLaNuevaFecha() {
        LocalDateTime original = manana(18);
        LocalDateTime nuevo = manana(18).plusDays(1);
        Clase clase = claseEnBase(PROFE_1, original, 60, 5, EstadoClase.PROGRAMADA);
        CambioEstado cambio = estados.cambiarEstado(clase, EstadoClase.REPROGRAMADA, "Lluvia", "SISTEMA", nuevo);

        avisos.avisar(cambio, TipoEvento.REPROGRAMACION, null, List.of(ALUMNO_1));

        String mensaje = notificaciones.eventos().get(0).mensaje();
        assertThat(mensaje).contains("se reprogramó al")
                .contains(String.format("%02d/%02d 18:00", nuevo.getDayOfMonth(), nuevo.getMonthValue()))
                .contains(String.format("%02d/%02d 18:00", original.getDayOfMonth(), original.getMonthValue()));
    }

    @Test
    void siSePasaUnMensajeSeUsaTalCual() {
        Clase clase = claseEnBase(PROFE_1, manana(18), 60, 5, EstadoClase.PROGRAMADA);
        CambioEstado cambio = suspender(clase);

        avisos.avisar(cambio, TipoEvento.SUSPENSION, "Mensaje propio", List.of(ALUMNO_1));

        assertThat(notificaciones.eventos().get(0).mensaje()).isEqualTo("Mensaje propio");
    }

    @Test
    void sinDestinatariosNoSeLlamaAMsNotificacionesYNadaQuedaPendiente() {
        Clase clase = claseEnBase(PROFE_1, manana(18), 60, 5, EstadoClase.PROGRAMADA);
        CambioEstado cambio = suspender(clase);

        avisos.avisar(cambio, TipoEvento.SUSPENSION, null, List.of());

        assertThat(notificaciones.eventos()).isEmpty();
        assertThat(cambios.findById(cambio.getId()).orElseThrow().isNotificado()).isTrue();
    }

    @Test
    void rn16_siFallaMsNotificacionesNoSeDeshaceElCambioDeEstado() {
        Clase clase = claseEnBase(PROFE_1, manana(18), 60, 5, EstadoClase.PROGRAMADA);
        CambioEstado cambio = suspender(clase);
        notificaciones.fallar(true);

        avisos.avisar(cambio, TipoEvento.SUSPENSION, null, List.of(ALUMNO_1)); // no debe lanzar excepción

        assertThat(clases.findById(clase.getId()).orElseThrow().getEstado()).isEqualTo(EstadoClase.SUSPENDIDA);
        assertThat(cambios.findById(cambio.getId()).orElseThrow().isNotificado()).isFalse();
        assertThat(notificaciones.eventos()).isEmpty();
    }

    @Test
    void rn16_reenviarPendientesUsaElMismoEventoIdYDespuesNoRepite() {
        Clase clase = claseEnBase(PROFE_1, manana(18), 60, 5, EstadoClase.PROGRAMADA);
        inscripcionEnBase(clase.getId(), ALUMNO_1);
        CambioEstado cambio = suspender(clase);
        notificaciones.fallar(true);
        avisos.avisar(cambio, TipoEvento.SUSPENSION, null, List.of(ALUMNO_1));
        notificaciones.fallar(false);

        int reenviados = avisos.reenviarPendientes();

        assertThat(reenviados).isEqualTo(1);
        assertThat(notificaciones.eventos()).hasSize(1);
        assertThat(notificaciones.eventos().get(0).eventoId()).isEqualTo(avisos.eventoId(cambio));
        assertThat(notificaciones.eventos().get(0).tipo()).isEqualTo(TipoEvento.SUSPENSION);
        assertThat(notificaciones.eventos().get(0).destinatarios()).containsExactly(ALUMNO_1);
        assertThat(cambios.findById(cambio.getId()).orElseThrow().isNotificado()).isTrue();

        assertThat(avisos.reenviarPendientes()).isZero(); // ya no queda nada pendiente
        assertThat(notificaciones.eventos()).hasSize(1);
    }

    @Test
    void rn16_siSigueFallandoElAvisoSigueSiendoPendiente() {
        Clase clase = claseEnBase(PROFE_1, manana(18), 60, 5, EstadoClase.PROGRAMADA);
        inscripcionEnBase(clase.getId(), ALUMNO_1);
        CambioEstado cambio = suspender(clase);
        notificaciones.fallar(true);

        assertThat(avisos.reenviarPendientes()).isZero();

        assertThat(cambios.findById(cambio.getId()).orElseThrow().isNotificado()).isFalse();
    }

    @Test
    void reenviarPendientesIgnoraLosCambiosQueNuncaLlevanAviso() {
        Clase clase = claseEnBase(PROFE_1, manana(18), 60, 5, EstadoClase.PROGRAMADA);
        estados.cambiarEstado(clase, EstadoClase.EN_RIESGO, "riesgo", "SISTEMA");
        Clase enRiesgo = clases.findById(clase.getId()).orElseThrow();
        CambioEstado vueltaAProgramada =
                estados.cambiarEstado(enRiesgo, EstadoClase.PROGRAMADA, "Mantenida", PROFE_1);

        avisos.reenviarPendientes();

        // El único aviso es la alerta de riesgo al profesor; volver a PROGRAMADA no genera ninguno
        assertThat(notificaciones.eventos()).hasSize(1);
        assertThat(notificaciones.eventos().get(0).tipo()).isEqualTo(TipoEvento.ALERTA_RIESGO);
        assertThat(notificaciones.eventos().get(0).destinatarios()).containsExactly(PROFE_1);
        assertThat(cambios.findById(vueltaAProgramada.getId()).orElseThrow().isNotificado()).isTrue();
    }
}
