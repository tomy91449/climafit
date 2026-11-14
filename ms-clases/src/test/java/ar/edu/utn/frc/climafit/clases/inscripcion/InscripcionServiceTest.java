package ar.edu.utn.frc.climafit.clases.inscripcion;

import ar.edu.utn.frc.climafit.clases.BaseIntegracionTest;
import ar.edu.utn.frc.climafit.clases.clase.Clase;
import ar.edu.utn.frc.climafit.clases.estado.EstadoClase;
import ar.edu.utn.frc.climafit.commons.error.NegocioException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class InscripcionServiceTest extends BaseIntegracionTest {

    @Autowired InscripcionService servicio;

    @Test
    void inscribirDejaLaInscripcionActiva() {
        Clase clase = claseEnBase(PROFE_1, manana(18), 60, 3, EstadoClase.PROGRAMADA);

        InscripcionResponse r = servicio.inscribir(clase.getId(), ALUMNO_1);

        assertThat(r.estado()).isEqualTo(EstadoInscripcion.ACTIVA);
        assertThat(r.claseId()).isEqualTo(clase.getId());
        assertThat(r.alumnoId()).isEqualTo(ALUMNO_1);
        assertThat(r.fechaAlta()).isNotNull();
    }

    @Test
    void rn01_rechazaInscripcionConCupoCompleto() {
        Clase clase = claseEnBase(PROFE_1, manana(18), 60, 2, EstadoClase.PROGRAMADA);
        servicio.inscribir(clase.getId(), ALUMNO_1);
        servicio.inscribir(clase.getId(), ALUMNO_2);

        assertError(() -> servicio.inscribir(clase.getId(), ALUMNO_3), "CUPO_COMPLETO", HttpStatus.CONFLICT);
    }

    @Test
    void rn01_noSobrepasaElCupoConInscripcionesSimultaneas() throws Exception {
        final Long claseId = claseEnBase(PROFE_1, manana(18), 60, 3, EstadoClase.PROGRAMADA).getId();
        final int intentos = 10;
        ExecutorService pool = Executors.newFixedThreadPool(intentos);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Boolean>> resultados = new ArrayList<>();
        for (int i = 0; i < intentos; i++) {
            final String alumno = "alumno-simultaneo-" + i;
            resultados.add(pool.submit(() -> {
                largada.await();
                try {
                    servicio.inscribir(claseId, alumno);
                    return true;
                } catch (NegocioException e) {
                    return false; // CUPO_COMPLETO
                }
            }));
        }
        largada.countDown(); // todos arrancan a la vez

        int exitosas = 0;
        for (Future<Boolean> f : resultados) {
            if (f.get(30, TimeUnit.SECONDS)) {
                exitosas++;
            }
        }
        pool.shutdown();

        assertThat(exitosas).isEqualTo(3);
        assertThat(inscripciones.countByClaseIdAndEstado(claseId, EstadoInscripcion.ACTIVA)).isEqualTo(3);
    }

    @Test
    void rn02_rechazaUnaClaseCancelada() {
        Clase clase = claseEnBase(PROFE_1, manana(18), 60, 5, EstadoClase.CANCELADA);

        assertError(() -> servicio.inscribir(clase.getId(), ALUMNO_1), "CLASE_NO_DISPONIBLE", HttpStatus.CONFLICT);
    }

    @Test
    void rn02_rechazaUnaClaseSuspendida() {
        Clase clase = claseEnBase(PROFE_1, manana(18), 60, 5, EstadoClase.SUSPENDIDA);

        assertError(() -> servicio.inscribir(clase.getId(), ALUMNO_1), "CLASE_NO_DISPONIBLE", HttpStatus.CONFLICT);
    }

    @Test
    void rn02_rechazaUnaClaseQueYaEmpezo() {
        Clase clase = claseEnBase(PROFE_1, LocalDateTime.now().minusMinutes(10), 60, 5, EstadoClase.PROGRAMADA);

        assertError(() -> servicio.inscribir(clase.getId(), ALUMNO_1), "CLASE_NO_DISPONIBLE", HttpStatus.CONFLICT);
    }

    @Test
    void rn02_admiteClasesEnRiesgoYReprogramadas() {
        Clase enRiesgo = claseEnBase(PROFE_1, manana(10), 60, 5, EstadoClase.EN_RIESGO);
        Clase reprogramada = claseEnBase(PROFE_1, manana(12), 60, 5, EstadoClase.REPROGRAMADA);

        assertThat(servicio.inscribir(enRiesgo.getId(), ALUMNO_1).estado()).isEqualTo(EstadoInscripcion.ACTIVA);
        assertThat(servicio.inscribir(reprogramada.getId(), ALUMNO_1).estado()).isEqualTo(EstadoInscripcion.ACTIVA);
    }

    @Test
    void rn02_rechazaInscribirseDosVecesALaMismaClase() {
        Clase clase = claseEnBase(PROFE_1, manana(18), 60, 5, EstadoClase.PROGRAMADA);
        servicio.inscribir(clase.getId(), ALUMNO_1);

        assertError(() -> servicio.inscribir(clase.getId(), ALUMNO_1), "YA_INSCRIPTO", HttpStatus.CONFLICT);
    }

    @Test
    void rn02_laCancelacionLiberaElLugarYElAlumnoPuedeVolverAInscribirse() {
        Clase clase = claseEnBase(PROFE_1, manana(18), 60, 1, EstadoClase.PROGRAMADA);
        InscripcionResponse primera = servicio.inscribir(clase.getId(), ALUMNO_1);

        InscripcionResponse cancelada = servicio.cancelar(primera.id(), ALUMNO_1);
        assertThat(cancelada.estado()).isEqualTo(EstadoInscripcion.CANCELADA);

        // el lugar quedó libre: otro alumno entra
        assertThat(servicio.inscribir(clase.getId(), ALUMNO_2).estado()).isEqualTo(EstadoInscripcion.ACTIVA);
        servicio.cancelar(servicio.listarMias(ALUMNO_2).get(0).id(), ALUMNO_2);

        // y el mismo alumno que canceló puede volver: se reactiva su misma fila
        InscripcionResponse otraVez = servicio.inscribir(clase.getId(), ALUMNO_1);
        assertThat(otraVez.id()).isEqualTo(primera.id());
        assertThat(otraVez.estado()).isEqualTo(EstadoInscripcion.ACTIVA);
    }

    @Test
    void rn02_noSePuedeCancelarUnaInscripcionDespuesDelInicio() {
        Clase clase = claseEnBase(PROFE_1, LocalDateTime.now().minusMinutes(5), 60, 5, EstadoClase.PROGRAMADA);
        Inscripcion inscripcion = inscripcionEnBase(clase.getId(), ALUMNO_1);

        assertError(() -> servicio.cancelar(inscripcion.getId(), ALUMNO_1), "CLASE_NO_DISPONIBLE", HttpStatus.CONFLICT);
    }

    @Test
    void rn03_rechazaUnaInscripcionQueSeSuperponeConOtraDelAlumno() {
        Clase a = claseEnBase(PROFE_1, manana(18), 60, 5, EstadoClase.PROGRAMADA);
        Clase b = claseEnBase(PROFE_2, manana(18).plusMinutes(30), 60, 5, EstadoClase.PROGRAMADA);
        servicio.inscribir(a.getId(), ALUMNO_1);

        assertError(() -> servicio.inscribir(b.getId(), ALUMNO_1), "SUPERPOSICION_ALUMNO", HttpStatus.CONFLICT);
    }

    @Test
    void rn03_permiteClasesConsecutivasSinSuperposicion() {
        Clase a = claseEnBase(PROFE_1, manana(18), 60, 5, EstadoClase.PROGRAMADA);
        Clase b = claseEnBase(PROFE_2, manana(19), 60, 5, EstadoClase.PROGRAMADA);
        servicio.inscribir(a.getId(), ALUMNO_1);

        assertThat(servicio.inscribir(b.getId(), ALUMNO_1).estado()).isEqualTo(EstadoInscripcion.ACTIVA);
    }

    @Test
    void rn03_unaInscripcionCanceladaNoCuentaParaLaSuperposicion() {
        Clase a = claseEnBase(PROFE_1, manana(18), 60, 5, EstadoClase.PROGRAMADA);
        Clase b = claseEnBase(PROFE_2, manana(18), 60, 5, EstadoClase.PROGRAMADA);
        InscripcionResponse enA = servicio.inscribir(a.getId(), ALUMNO_1);
        servicio.cancelar(enA.id(), ALUMNO_1);

        assertThat(servicio.inscribir(b.getId(), ALUMNO_1).estado()).isEqualTo(EstadoInscripcion.ACTIVA);
    }

    @Test
    void cancelarLaInscripcionDeOtroAlumnoResponde403() {
        Clase clase = claseEnBase(PROFE_1, manana(18), 60, 5, EstadoClase.PROGRAMADA);
        Inscripcion inscripcion = inscripcionEnBase(clase.getId(), ALUMNO_1);

        assertError(() -> servicio.cancelar(inscripcion.getId(), ALUMNO_2), "ACCESO_DENEGADO", HttpStatus.FORBIDDEN);
    }

    @Test
    void inscribirseEnUnaClaseInexistenteResponde404() {
        assertError(() -> servicio.inscribir(999_999L, ALUMNO_1), "NO_ENCONTRADO", HttpStatus.NOT_FOUND);
    }

    @Test
    void rn05_soloElProfesorDuenoOElAdminVeLosInscriptos() {
        Clase clase = claseEnBase(PROFE_1, manana(18), 60, 5, EstadoClase.PROGRAMADA);
        servicio.inscribir(clase.getId(), ALUMNO_1);

        assertThat(servicio.listarInscriptos(clase.getId(), PROFE_1, false)).hasSize(1);
        assertThat(servicio.listarInscriptos(clase.getId(), "admin1-sub", true)).hasSize(1);
        assertError(() -> servicio.listarInscriptos(clase.getId(), PROFE_2, false),
                "ACCESO_DENEGADO", HttpStatus.FORBIDDEN);
    }

    @Test
    void misInscripcionesDevuelveSoloLasDelAlumno() {
        Clase a = claseEnBase(PROFE_1, manana(10), 60, 5, EstadoClase.PROGRAMADA);
        Clase b = claseEnBase(PROFE_1, manana(12), 60, 5, EstadoClase.PROGRAMADA);
        servicio.inscribir(a.getId(), ALUMNO_1);
        servicio.inscribir(b.getId(), ALUMNO_2);

        assertThat(servicio.listarMias(ALUMNO_1)).extracting(InscripcionResponse::claseId)
                .containsExactly(a.getId());
    }
}
