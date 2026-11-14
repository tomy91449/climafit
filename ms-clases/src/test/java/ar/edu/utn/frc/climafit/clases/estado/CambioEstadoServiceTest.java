package ar.edu.utn.frc.climafit.clases.estado;

import ar.edu.utn.frc.climafit.clases.BaseIntegracionTest;
import ar.edu.utn.frc.climafit.clases.clase.Clase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CambioEstadoServiceTest extends BaseIntegracionTest {

    @Autowired CambioEstadoService estados;

    @Test
    void transicionPermitidaCambiaElEstadoYRegistraElHistorial() {
        Clase clase = claseEnBase(PROFE_1, manana(18), 60, 10, EstadoClase.PROGRAMADA);

        CambioEstado cambio = estados.cambiarEstado(clase, EstadoClase.SUSPENDIDA, "Lluvia", "SISTEMA");

        assertThat(clases.findById(clase.getId()).orElseThrow().getEstado()).isEqualTo(EstadoClase.SUSPENDIDA);
        assertThat(cambio.getEstadoAnterior()).isEqualTo(EstadoClase.PROGRAMADA);
        assertThat(cambio.getEstadoNuevo()).isEqualTo(EstadoClase.SUSPENDIDA);
        assertThat(cambio.getMotivo()).isEqualTo("Lluvia");
        assertThat(cambio.getResponsable()).isEqualTo("SISTEMA");
        assertThat(cambio.isNotificado()).isFalse();
    }

    @Test
    void transicionProhibidaSeRechazaYNoCambiaNada() {
        Clase clase = claseEnBase(PROFE_1, manana(18), 60, 10, EstadoClase.PROGRAMADA);

        assertError(() -> estados.cambiarEstado(clase, EstadoClase.SUSPENDIDA_SIN_FECHA, "x", PROFE_1),
                "CLASE_NO_MODIFICABLE", HttpStatus.CONFLICT);

        assertThat(clases.findById(clase.getId()).orElseThrow().getEstado()).isEqualTo(EstadoClase.PROGRAMADA);
        assertThat(cambios.findAll()).isEmpty();
    }

    @Test
    void rn06_rechazaCambiosEnUnaClaseCancelada() {
        Clase clase = claseEnBase(PROFE_1, manana(18), 60, 10, EstadoClase.CANCELADA);

        assertError(() -> estados.cambiarEstado(clase, EstadoClase.REPROGRAMADA, "x", PROFE_1),
                "CLASE_NO_MODIFICABLE", HttpStatus.CONFLICT);
    }

    @Test
    void rn06_rechazaCambiosEnUnaClaseCuyoInicioYaPaso() {
        Clase clase = claseEnBase(PROFE_1, LocalDateTime.now().minusHours(2), 60, 10, EstadoClase.PROGRAMADA);

        assertError(() -> estados.cambiarEstado(clase, EstadoClase.CANCELADA, "x", PROFE_1),
                "CLASE_NO_MODIFICABLE", HttpStatus.CONFLICT);
    }

    @Test
    void rn06_unaClaseSuspendidaSePuedeReagendarAunqueSuInicioViejoYaPaso() {
        Clase clase = claseEnBase(PROFE_1, LocalDateTime.now().minusHours(2), 60, 10, EstadoClase.SUSPENDIDA_SIN_FECHA);
        LocalDateTime nuevo = manana(10);

        estados.cambiarEstado(clase, EstadoClase.REPROGRAMADA, "Reagendada", PROFE_1, nuevo);

        Clase guardada = clases.findById(clase.getId()).orElseThrow();
        assertThat(guardada.getEstado()).isEqualTo(EstadoClase.REPROGRAMADA);
        assertThat(guardada.getInicio()).isEqualTo(nuevo);
    }

    @Test
    void alPasarAEnRiesgoGuardaElEstadoPrevio() {
        Clase clase = claseEnBase(PROFE_1, manana(18), 60, 10, EstadoClase.REPROGRAMADA);

        estados.cambiarEstado(clase, EstadoClase.EN_RIESGO, "Viento cerca del límite", "SISTEMA");

        Clase guardada = clases.findById(clase.getId()).orElseThrow();
        assertThat(guardada.getEstado()).isEqualTo(EstadoClase.EN_RIESGO);
        assertThat(guardada.getEstadoPrevio()).isEqualTo(EstadoClase.REPROGRAMADA);
    }

    @Test
    void desdeEnRiesgoSePuedeVolverAlEstadoPrevio() {
        Clase clase = claseEnBase(PROFE_1, manana(18), 60, 10, EstadoClase.PROGRAMADA);
        estados.cambiarEstado(clase, EstadoClase.EN_RIESGO, "riesgo", "SISTEMA");
        Clase enRiesgo = clases.findById(clase.getId()).orElseThrow();

        estados.cambiarEstado(enRiesgo, EstadoClase.PROGRAMADA, "El profesor decidió mantenerla", PROFE_1);

        assertThat(clases.findById(clase.getId()).orElseThrow().getEstado()).isEqualTo(EstadoClase.PROGRAMADA);
    }

    @Test
    void laReprogramacionGuardaInicioAnteriorEInicioNuevo() {
        LocalDateTime original = manana(18);
        LocalDateTime nuevo = manana(18).plusDays(1);
        Clase clase = claseEnBase(PROFE_1, original, 60, 10, EstadoClase.PROGRAMADA);

        CambioEstado cambio = estados.cambiarEstado(clase, EstadoClase.REPROGRAMADA, "Lluvia", "SISTEMA", nuevo);

        assertThat(cambio.getInicioAnterior()).isEqualTo(original);
        assertThat(cambio.getInicioNuevo()).isEqualTo(nuevo);
        assertThat(clases.findById(clase.getId()).orElseThrow().getInicio()).isEqualTo(nuevo);
    }

    @Test
    void elHistorialQuedaEnOrdenProgramadaSuspendidaReprogramada() {
        Clase clase = claseEnBase(PROFE_1, manana(18), 60, 10, EstadoClase.PROGRAMADA);
        estados.registrarCreacion(clase, PROFE_1);
        estados.cambiarEstado(clase, EstadoClase.SUSPENDIDA, "Lluvia", "SISTEMA");
        Clase suspendida = clases.findById(clase.getId()).orElseThrow();
        estados.cambiarEstado(suspendida, EstadoClase.REPROGRAMADA, "Nuevo día apto", "SISTEMA", manana(18).plusDays(1));

        List<CambioEstado> historial = cambios.findByClaseIdOrderByFechaAscIdAsc(clase.getId());

        assertThat(historial).extracting(CambioEstado::getEstadoNuevo)
                .containsExactly(EstadoClase.PROGRAMADA, EstadoClase.SUSPENDIDA, EstadoClase.REPROGRAMADA);
        assertThat(historial.get(0).getEstadoAnterior()).isNull();
    }
}
