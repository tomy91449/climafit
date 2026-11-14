package ar.edu.utn.frc.climafit.clases.clase;

import ar.edu.utn.frc.climafit.clases.BaseIntegracionTest;
import ar.edu.utn.frc.climafit.clases.aviso.EventoNotificacion;
import ar.edu.utn.frc.climafit.clases.aviso.TipoEvento;
import ar.edu.utn.frc.climafit.clases.estado.CambioEstado;
import ar.edu.utn.frc.climafit.clases.estado.EstadoClase;
import ar.edu.utn.frc.climafit.clases.inscripcion.EstadoInscripcion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClaseServiceTest extends BaseIntegracionTest {

    @Autowired ClaseService servicio;

    private ClaseRequest pedido(Long actividadId, Long espacioId, LocalDateTime inicio, int duracion, int cupo,
                                List<RutinaItemDto> rutina) {
        return new ClaseRequest(actividadId, espacioId, inicio, duracion, cupo, rutina);
    }

    private ClaseRequest yogaEnParque(LocalDateTime inicio) {
        return pedido(1L, 1L, inicio, 60, 10, List.of(new RutinaItemDto(1L, 1, "Calentar")));
    }

    @Test
    void laClaseNaceProgramadaCopiaAlAireLibreYRegistraSuPrimerCambio() {
        ClaseResponse r = servicio.crear(yogaEnParque(manana(18)), PROFE_1);

        assertThat(r.estado()).isEqualTo(EstadoClase.PROGRAMADA);
        assertThat(r.profesorId()).isEqualTo(PROFE_1);
        assertThat(r.alAireLibre()).isTrue(); // Parque Sarmiento
        assertThat(r.lugaresDisponibles()).isEqualTo(10);
        assertThat(r.rutina()).hasSize(1);

        List<CambioEstado> historial = cambios.findByClaseIdOrderByFechaAscIdAsc(r.id());
        assertThat(historial).hasSize(1);
        assertThat(historial.get(0).getEstadoAnterior()).isNull();
        assertThat(historial.get(0).getEstadoNuevo()).isEqualTo(EstadoClase.PROGRAMADA);
    }

    @Test
    void unEspacioTechadoCopiaAlAireLibreEnFalso() {
        ClaseResponse r = servicio.crear(pedido(1L, 3L, manana(18), 60, 10, List.of()), PROFE_1);

        assertThat(r.alAireLibre()).isFalse(); // Gimnasio Centro
    }

    @Test
    void siElEspacioNoExisteResponde404() {
        assertError(() -> servicio.crear(pedido(1L, 99L, manana(18), 60, 10, List.of()), PROFE_1),
                "NO_ENCONTRADO", HttpStatus.NOT_FOUND);
    }

    @Test
    void rn04_rechazaUnaClaseQueSeSuperponeConOtraDelProfesor() {
        servicio.crear(yogaEnParque(manana(18)), PROFE_1);

        // 18:30 cae dentro de la clase de 18:00 a 19:00
        assertError(() -> servicio.crear(yogaEnParque(manana(18).plusMinutes(30)), PROFE_1),
                "SUPERPOSICION_PROFESOR", HttpStatus.CONFLICT);
    }

    @Test
    void rn04_permiteUnaClaseJustoCuandoTerminaLaAnterior() {
        servicio.crear(yogaEnParque(manana(18)), PROFE_1);

        ClaseResponse r = servicio.crear(yogaEnParque(manana(19)), PROFE_1);

        assertThat(r.id()).isNotNull();
    }

    @Test
    void rn04_otroProfesorPuedeDarClaseAlMismoHorario() {
        servicio.crear(yogaEnParque(manana(18)), PROFE_1);

        ClaseResponse r = servicio.crear(yogaEnParque(manana(18)), PROFE_2);

        assertThat(r.profesorId()).isEqualTo(PROFE_2);
    }

    @Test
    void rn04_unaClaseCanceladaNoCuentaParaLaSuperposicion() {
        claseEnBase(PROFE_1, manana(18), 60, 10, EstadoClase.CANCELADA);

        ClaseResponse r = servicio.crear(yogaEnParque(manana(18)), PROFE_1);

        assertThat(r.id()).isNotNull();
    }

    @Test
    void rn07_rechazaUnaRutinaConUnEjercicioIncompatible() {
        // En el stub, el ejercicio 3 (Burpees) es incompatible con Yoga
        ClaseRequest pedido = pedido(1L, 1L, manana(18), 60, 10, List.of(new RutinaItemDto(3L, 1, null)));

        assertError(() -> servicio.crear(pedido, PROFE_1), "EJERCICIO_INCOMPATIBLE", HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(clases.findAll()).isEmpty();
    }

    @Test
    void rn07_tambienSeVerificaAlModificarLaRutina() {
        ClaseResponse creada = servicio.crear(yogaEnParque(manana(18)), PROFE_1);
        ClaseRequest cambio = new ClaseRequest(null, null, null, null, null, List.of(new RutinaItemDto(3L, 1, null)));

        assertError(() -> servicio.modificar(creada.id(), cambio, PROFE_1, false),
                "EJERCICIO_INCOMPATIBLE", HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void rn05_unProfesorAjenoNoPuedeCancelarLaClase() {
        ClaseResponse creada = servicio.crear(yogaEnParque(manana(18)), PROFE_1);

        assertError(() -> servicio.cancelar(creada.id(), "x", PROFE_2, false),
                "ACCESO_DENEGADO", HttpStatus.FORBIDDEN);
    }

    @Test
    void rn05_unProfesorAjenoNoPuedeModificarNiReprogramar() {
        ClaseResponse creada = servicio.crear(yogaEnParque(manana(18)), PROFE_1);

        assertError(() -> servicio.modificar(creada.id(), new ClaseRequest(null, null, null, null, 5, null), PROFE_2, false),
                "ACCESO_DENEGADO", HttpStatus.FORBIDDEN);
        assertError(() -> servicio.reprogramar(creada.id(), manana(20), PROFE_2),
                "ACCESO_DENEGADO", HttpStatus.FORBIDDEN);
    }

    @Test
    void rn05_elAdminPuedeCancelarClasesAjenas() {
        ClaseResponse creada = servicio.crear(yogaEnParque(manana(18)), PROFE_1);

        ClaseResponse r = servicio.cancelar(creada.id(), "Mantenimiento del parque", "admin1-sub", true);

        assertThat(r.estado()).isEqualTo(EstadoClase.CANCELADA);
    }

    @Test
    void rn05_elAdminNoPuedeReprogramarPorqueEseEndpointEsSoloDelProfesorDueno() {
        ClaseResponse creada = servicio.crear(yogaEnParque(manana(18)), PROFE_1);

        assertError(() -> servicio.reprogramar(creada.id(), manana(20), "admin1-sub"),
                "ACCESO_DENEGADO", HttpStatus.FORBIDDEN);
    }

    @Test
    void rn06_noSePuedeCancelarUnaClaseCuyoInicioYaPaso() {
        var pasada = claseEnBase(PROFE_1, LocalDateTime.now().minusHours(3), 60, 10, EstadoClase.PROGRAMADA);

        assertError(() -> servicio.cancelar(pasada.getId(), "x", PROFE_1, false),
                "CLASE_NO_MODIFICABLE", HttpStatus.CONFLICT);
    }

    @Test
    void rn06_noSePuedeModificarUnaClaseCancelada() {
        var cancelada = claseEnBase(PROFE_1, manana(18), 60, 10, EstadoClase.CANCELADA);

        assertError(() -> servicio.modificar(cancelada.getId(), new ClaseRequest(null, null, null, null, 5, null),
                        PROFE_1, false),
                "CLASE_NO_MODIFICABLE", HttpStatus.CONFLICT);
    }

    @Test
    void cancelarCancelaLasInscripcionesYAvisaALosInscriptos() {
        var clase = claseEnBase(PROFE_1, manana(18), 60, 10, EstadoClase.PROGRAMADA);
        inscripcionEnBase(clase.getId(), ALUMNO_1);
        inscripcionEnBase(clase.getId(), ALUMNO_2);

        ClaseResponse r = servicio.cancelar(clase.getId(), "Profesor enfermo", PROFE_1, false);

        assertThat(r.estado()).isEqualTo(EstadoClase.CANCELADA);
        assertThat(inscripciones.findByClaseIdAndEstado(clase.getId(), EstadoInscripcion.ACTIVA)).isEmpty();
        assertThat(inscripciones.findByClaseIdAndEstado(clase.getId(), EstadoInscripcion.CANCELADA)).hasSize(2);

        assertThat(notificaciones.eventos()).hasSize(1);
        EventoNotificacion evento = notificaciones.eventos().get(0);
        assertThat(evento.tipo()).isEqualTo(TipoEvento.CANCELACION);
        assertThat(evento.destinatarios()).containsExactlyInAnyOrder(ALUMNO_1, ALUMNO_2);
        assertThat(evento.mensaje()).contains("cancelada").contains("Profesor enfermo");
        assertThat(evento.profesorId()).isEqualTo(PROFE_1);
    }

    @Test
    void reprogramarMueveLaClaseGuardaElHistorialYAvisa() {
        var clase = claseEnBase(PROFE_1, manana(18), 60, 10, EstadoClase.PROGRAMADA);
        inscripcionEnBase(clase.getId(), ALUMNO_1);
        LocalDateTime nuevo = manana(18).plusDays(1);

        ClaseResponse r = servicio.reprogramar(clase.getId(), nuevo, PROFE_1);

        assertThat(r.estado()).isEqualTo(EstadoClase.REPROGRAMADA);
        assertThat(r.inicio()).isEqualTo(nuevo);
        CambioEstado cambio = cambios.findByClaseIdOrderByFechaAscIdAsc(clase.getId()).get(0);
        assertThat(cambio.getInicioAnterior()).isEqualTo(manana(18));
        assertThat(cambio.getInicioNuevo()).isEqualTo(nuevo);
        assertThat(cambio.isNotificado()).isTrue();

        assertThat(notificaciones.eventos()).hasSize(1);
        assertThat(notificaciones.eventos().get(0).tipo()).isEqualTo(TipoEvento.REPROGRAMACION);
        assertThat(notificaciones.eventos().get(0).eventoId())
                .isEqualTo("CLASE-" + clase.getId() + "-CAMBIO-" + cambio.getId());
    }

    @Test
    void reprogramarDesdeSuspendidaSinFechaVuelveAReprogramada() {
        var clase = claseEnBase(PROFE_1, manana(18), 60, 10, EstadoClase.SUSPENDIDA_SIN_FECHA);

        ClaseResponse r = servicio.reprogramar(clase.getId(), manana(18).plusDays(2), PROFE_1);

        assertThat(r.estado()).isEqualTo(EstadoClase.REPROGRAMADA);
    }

    @Test
    void reprogramarAUnaFechaPasadaSeRechaza() {
        var clase = claseEnBase(PROFE_1, manana(18), 60, 10, EstadoClase.PROGRAMADA);

        assertError(() -> servicio.reprogramar(clase.getId(), LocalDateTime.now().minusDays(1), PROFE_1),
                "DATOS_INVALIDOS", HttpStatus.BAD_REQUEST);
    }

    @Test
    void rn04_reprogramarNoPuedeSuperponerseConOtraClaseDelProfesor() {
        claseEnBase(PROFE_1, manana(20), 60, 10, EstadoClase.PROGRAMADA);
        var clase = claseEnBase(PROFE_1, manana(18), 60, 10, EstadoClase.PROGRAMADA);

        assertError(() -> servicio.reprogramar(clase.getId(), manana(20).plusMinutes(15), PROFE_1),
                "SUPERPOSICION_PROFESOR", HttpStatus.CONFLICT);
    }

    @Test
    void noSePuedeBajarElCupoPorDebajoDeLosInscriptos() {
        var clase = claseEnBase(PROFE_1, manana(18), 60, 10, EstadoClase.PROGRAMADA);
        inscripcionEnBase(clase.getId(), ALUMNO_1);
        inscripcionEnBase(clase.getId(), ALUMNO_2);

        assertError(() -> servicio.modificar(clase.getId(), new ClaseRequest(null, null, null, null, 1, null),
                        PROFE_1, false),
                "CUPO_COMPLETO", HttpStatus.CONFLICT);
    }

    @Test
    void modificarCambiaCupoDuracionYRutina() {
        ClaseResponse creada = servicio.crear(yogaEnParque(manana(18)), PROFE_1);
        ClaseRequest cambio = new ClaseRequest(null, null, null, 90, 20,
                List.of(new RutinaItemDto(2L, 1, "Equilibrio"), new RutinaItemDto(1L, 2, null)));

        ClaseResponse r = servicio.modificar(creada.id(), cambio, PROFE_1, false);

        assertThat(r.duracionMinutos()).isEqualTo(90);
        assertThat(r.cupo()).isEqualTo(20);
        assertThat(r.rutina()).extracting(RutinaItemDto::ejercicioId).containsExactly(2L, 1L);
        assertThat(r.inicio()).isEqualTo(creada.inicio()); // el inicio no se toca desde el PUT
    }

    @Test
    void buscarFiltraPorActividadYEstadoYMuestraSoloClasesConLugares() {
        var yoga = claseEnBase(PROFE_1, manana(10), 60, 5, EstadoClase.PROGRAMADA);
        var llena = claseEnBase(PROFE_1, manana(12), 60, 1, EstadoClase.PROGRAMADA);
        inscripcionEnBase(llena.getId(), ALUMNO_1);
        var cancelada = claseEnBase(PROFE_1, manana(14), 60, 5, EstadoClase.CANCELADA);

        List<ClaseResponse> todas = servicio.buscar(1L, null, null, null, null);
        List<ClaseResponse> programadas = servicio.buscar(1L, null, null, null, EstadoClase.PROGRAMADA);

        assertThat(todas).extracting(ClaseResponse::id).containsExactly(yoga.getId(), cancelada.getId());
        assertThat(programadas).extracting(ClaseResponse::id).containsExactly(yoga.getId());
        assertThat(servicio.buscar(2L, null, null, null, null)).isEmpty();
    }

    @Test
    void buscarAceptaFechasDeUnSoloDia() {
        var clase = claseEnBase(PROFE_1, manana(10), 60, 5, EstadoClase.PROGRAMADA);
        String dia = manana(10).toLocalDate().toString();

        assertThat(servicio.buscar(null, null, dia, dia, null)).extracting(ClaseResponse::id)
                .containsExactly(clase.getId());
        assertThat(servicio.buscar(null, null, manana(10).plusDays(1).toLocalDate().toString(), null, null)).isEmpty();
    }

    @Test
    void buscarConUnaFechaInvalidaResponde400() {
        assertError(() -> servicio.buscar(null, null, "ayer", null, null), "DATOS_INVALIDOS", HttpStatus.BAD_REQUEST);
    }

    @Test
    void obtenerUnaClaseInexistenteResponde404() {
        assertError(() -> servicio.obtener(999_999L), "NO_ENCONTRADO", HttpStatus.NOT_FOUND);
    }
}
