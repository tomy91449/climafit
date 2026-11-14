package ar.edu.utn.frc.climafit.clases.evaluacion;

import static ar.edu.utn.frc.climafit.clases.evaluacion.Datos.SARMIENTO;
import static ar.edu.utn.frc.climafit.clases.evaluacion.Datos.YOGA;
import static ar.edu.utn.frc.climafit.clases.evaluacion.Datos.clase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.climafit.clases.clase.Clase;
import ar.edu.utn.frc.climafit.clases.client.actividades.ActividadesClient;
import ar.edu.utn.frc.climafit.clases.estado.CambioEstadoService;
import ar.edu.utn.frc.climafit.clases.estado.EstadoClase;
import ar.edu.utn.frc.climafit.clases.reprogramacion.PlanReprogramacion;
import ar.edu.utn.frc.climafit.clases.reprogramacion.ReprogramacionService;
import ar.edu.utn.frc.climafit.commons.error.ServicioNoDisponibleException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** RN12: decisión del profesor sobre una clase EN_RIESGO. */
class DecisionRiesgoServiceTest {

    private CambioEstadoService cambioEstado;
    private ReprogramacionService reprogramacion;
    private EvaluacionClimaticaRepository evaluaciones;
    private DecisionRiesgoService servicio;

    @BeforeEach
    void preparar() {
        cambioEstado = mock(CambioEstadoService.class);
        reprogramacion = mock(ReprogramacionService.class);
        evaluaciones = mock(EvaluacionClimaticaRepository.class);
        ActividadesClient actividades = mock(ActividadesClient.class);
        when(actividades.obtenerActividad(1L)).thenReturn(YOGA);
        when(actividades.obtenerEspacio(1L)).thenReturn(SARMIENTO);
        when(evaluaciones.findFirstByClaseIdAndResultadoNotOrderByFechaEvaluacionDesc(any(), eq(ResultadoEvaluacion.FALLIDA)))
                .thenReturn(Optional.empty());
        servicio = new DecisionRiesgoService(cambioEstado, actividades, reprogramacion, evaluaciones,
                Clock.fixed(Instant.parse("2026-11-14T19:00:00Z"), ZoneId.of("America/Argentina/Cordoba")));
    }

    private Clase enRiesgo(EstadoClase previo) {
        Clase c = clase(1, 6);
        c.setEstado(EstadoClase.EN_RIESGO);
        c.setEstadoPrevio(previo);
        return c;
    }

    @Test
    void rn12_decidirSobreUnaClaseQueNoEstaEnRiesgoDa409() {
        ClaseNoEnRiesgoException e = assertThrows(ClaseNoEnRiesgoException.class,
                () -> servicio.decidir(clase(1, 6), DecisionRiesgo.MANTENER, "profe1"));
        assertEquals("CLASE_NO_EN_RIESGO", e.getCodigo());
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
    }

    @Test
    void rn12_mantenerDevuelveLaClaseASuEstadoPrevio() {
        Clase c = enRiesgo(EstadoClase.REPROGRAMADA);
        servicio.decidir(c, DecisionRiesgo.MANTENER, "profe1");
        verify(cambioEstado).cambiarEstado(eq(c), eq(EstadoClase.REPROGRAMADA), anyString(), eq("profe1"));
        verify(reprogramacion, never()).buscarNuevoDia(any(), any(), any());
    }

    @Test
    void rn12_suspenderBuscaNuevoDiaYSigueConLaReprogramacion() {
        Clase c = enRiesgo(EstadoClase.PROGRAMADA);
        PlanReprogramacion plan = new PlanReprogramacion(c.getInicio().plusDays(1));
        when(reprogramacion.buscarNuevoDia(c, YOGA, SARMIENTO)).thenReturn(plan);

        servicio.decidir(c, DecisionRiesgo.SUSPENDER, "profe1");

        verify(reprogramacion).suspenderYReprogramar(eq(c), eq(YOGA), eq(plan), anyString(), eq("profe1"));
    }

    /** Verifica que una dependencia caída se registra como FALLIDA y no cambia el estado. */
    @Test
    void rn16_siFallaUnServicioAlSuspenderRegistraEvaluacionFallidaYConservaElRiesgo() {
        Clase c = enRiesgo(EstadoClase.PROGRAMADA);
        when(reprogramacion.buscarNuevoDia(c, YOGA, SARMIENTO)).thenThrow(new ServicioNoDisponibleException("caído"));
        assertThrows(ServicioNoDisponibleException.class,
                () -> servicio.decidir(c, DecisionRiesgo.SUSPENDER, "profe1"));
        org.mockito.ArgumentCaptor<EvaluacionClimatica> captor =
                org.mockito.ArgumentCaptor.forClass(EvaluacionClimatica.class);
        verify(evaluaciones).save(captor.capture());
        assertEquals(ResultadoEvaluacion.FALLIDA, captor.getValue().getResultado());
        assertEquals(c.getInicio(), captor.getValue().getInicioEvaluado());
        verify(reprogramacion, never()).suspenderYReprogramar(any(), any(), any(), anyString(), anyString());
        assertEquals(EstadoClase.EN_RIESGO, c.getEstado());
    }
}
