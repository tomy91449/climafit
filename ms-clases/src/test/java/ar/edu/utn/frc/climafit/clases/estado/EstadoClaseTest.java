package ar.edu.utn.frc.climafit.clases.estado;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static ar.edu.utn.frc.climafit.clases.estado.EstadoClase.*;
import static org.assertj.core.api.Assertions.assertThat;

/** Una prueba por cada transición permitida de la propuesta (4.2) y una por cada prohibida. */
class EstadoClaseTest {

    @ParameterizedTest(name = "{0} -> {1} está permitida")
    @CsvSource({
            "PROGRAMADA,EN_RIESGO", "PROGRAMADA,SUSPENDIDA", "PROGRAMADA,REPROGRAMADA", "PROGRAMADA,CANCELADA",
            "REPROGRAMADA,EN_RIESGO", "REPROGRAMADA,SUSPENDIDA", "REPROGRAMADA,REPROGRAMADA", "REPROGRAMADA,CANCELADA",
            "EN_RIESGO,SUSPENDIDA", "EN_RIESGO,CANCELADA",
            "SUSPENDIDA,REPROGRAMADA", "SUSPENDIDA,SUSPENDIDA_SIN_FECHA", "SUSPENDIDA,CANCELADA",
            "SUSPENDIDA_SIN_FECHA,REPROGRAMADA", "SUSPENDIDA_SIN_FECHA,CANCELADA"
    })
    void transicionPermitida(EstadoClase desde, EstadoClase hacia) {
        assertThat(desde.permiteTransicionA(hacia, null)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1} está prohibida")
    @CsvSource({
            "PROGRAMADA,PROGRAMADA", "PROGRAMADA,SUSPENDIDA_SIN_FECHA",
            "REPROGRAMADA,PROGRAMADA", "REPROGRAMADA,SUSPENDIDA_SIN_FECHA",
            "EN_RIESGO,EN_RIESGO", "EN_RIESGO,REPROGRAMADA", "EN_RIESGO,SUSPENDIDA_SIN_FECHA", "EN_RIESGO,PROGRAMADA",
            "SUSPENDIDA,PROGRAMADA", "SUSPENDIDA,EN_RIESGO", "SUSPENDIDA,SUSPENDIDA",
            "SUSPENDIDA_SIN_FECHA,PROGRAMADA", "SUSPENDIDA_SIN_FECHA,EN_RIESGO", "SUSPENDIDA_SIN_FECHA,SUSPENDIDA",
            "CANCELADA,PROGRAMADA", "CANCELADA,EN_RIESGO", "CANCELADA,SUSPENDIDA", "CANCELADA,REPROGRAMADA",
            "CANCELADA,SUSPENDIDA_SIN_FECHA", "CANCELADA,CANCELADA"
    })
    void transicionProhibida(EstadoClase desde, EstadoClase hacia) {
        assertThat(desde.permiteTransicionA(hacia, null)).isFalse();
    }

    @Test
    void enRiesgoVuelveAlEstadoPrevioQueTenia() {
        assertThat(EN_RIESGO.permiteTransicionA(PROGRAMADA, PROGRAMADA)).isTrue();
        assertThat(EN_RIESGO.permiteTransicionA(REPROGRAMADA, REPROGRAMADA)).isTrue();
    }

    @Test
    void enRiesgoNoVuelveAUnEstadoQueNoEraElPrevio() {
        assertThat(EN_RIESGO.permiteTransicionA(REPROGRAMADA, PROGRAMADA)).isFalse();
        assertThat(EN_RIESGO.permiteTransicionA(PROGRAMADA, REPROGRAMADA)).isFalse();
    }

    @Test
    void soloLosEstadosVigentesAdmitenInscripciones() {
        assertThat(PROGRAMADA.esVigente()).isTrue();
        assertThat(EN_RIESGO.esVigente()).isTrue();
        assertThat(REPROGRAMADA.esVigente()).isTrue();
        assertThat(SUSPENDIDA.esVigente()).isFalse();
        assertThat(SUSPENDIDA_SIN_FECHA.esVigente()).isFalse();
        assertThat(CANCELADA.esVigente()).isFalse();
    }
}
