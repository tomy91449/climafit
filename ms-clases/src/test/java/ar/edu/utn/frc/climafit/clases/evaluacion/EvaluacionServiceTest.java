package ar.edu.utn.frc.climafit.clases.evaluacion;

import static ar.edu.utn.frc.climafit.clases.evaluacion.Datos.AHORA;
import static ar.edu.utn.frc.climafit.clases.evaluacion.Datos.RELOJ;
import static ar.edu.utn.frc.climafit.clases.evaluacion.Datos.SARMIENTO;
import static ar.edu.utn.frc.climafit.clases.evaluacion.Datos.YOGA;
import static ar.edu.utn.frc.climafit.clases.evaluacion.Datos.cambio;
import static ar.edu.utn.frc.climafit.clases.evaluacion.Datos.clase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.climafit.clases.aviso.AvisoService;
import ar.edu.utn.frc.climafit.clases.aviso.TipoEvento;
import ar.edu.utn.frc.climafit.clases.clase.Clase;
import ar.edu.utn.frc.climafit.clases.client.actividades.ActividadesClient;
import ar.edu.utn.frc.climafit.clases.client.clima.ClimaClientStub;
import ar.edu.utn.frc.climafit.clases.estado.CambioEstadoService;
import ar.edu.utn.frc.climafit.clases.estado.EstadoClase;
import ar.edu.utn.frc.climafit.clases.reprogramacion.PlanReprogramacion;
import ar.edu.utn.frc.climafit.clases.reprogramacion.ReprogramacionService;
import ar.edu.utn.frc.climafit.clases.reprogramacion.ResultadoReprogramacion;
import ar.edu.utn.frc.climafit.commons.error.ServicioNoDisponibleException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** RN09, RN11 y RN16 del proceso de evaluación, con los clientes falsos (ClimaClientStub). */
class EvaluacionServiceTest {

    private ActividadesClient actividades;
    private ClimaClientStub clima;
    private EvaluacionClimaticaRepository evaluaciones;
    private CambioEstadoService cambioEstado;
    private AvisoService aviso;
    private ReprogramacionService reprogramacion;
    private EvaluacionService servicio;

    @BeforeEach
    void preparar() {
        actividades = mock(ActividadesClient.class);
        when(actividades.obtenerActividad(1L)).thenReturn(YOGA);
        when(actividades.obtenerEspacio(1L)).thenReturn(SARMIENTO);
        clima = new ClimaClientStub();
        evaluaciones = mock(EvaluacionClimaticaRepository.class);
        cambioEstado = mock(CambioEstadoService.class);
        aviso = mock(AvisoService.class);
        reprogramacion = mock(ReprogramacionService.class);
        servicio = new EvaluacionService(actividades, clima, evaluaciones, cambioEstado, aviso, reprogramacion, RELOJ);
    }

    // ---------- RN09 ----------

    @Test
    void rn09_claseTechadaNoSeEvalua() {
        Clase techada = Datos.clase(5L, 3L, false, AHORA.plusHours(6));
        ClaseNoEvaluableException e = assertThrows(ClaseNoEvaluableException.class,
                () -> servicio.evaluarManual(techada, "profe1"));
        assertEquals("CLASE_NO_EVALUABLE", e.getCodigo());
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        assertTrue(clima.llamadas().isEmpty());
    }

    @Test
    void rn09_claseQueEmpiezaDespuesDeLas24HorasNoSeEvalua() {
        assertThrows(ClaseNoEvaluableException.class, () -> servicio.evaluarManual(clase(1, 25), "profe1"));
    }

    @Test
    void rn09_claseQueEmpiezaExactamenteEn24HorasSiSeEvalua() {
        assertEquals(ResultadoEvaluacion.APTA, servicio.evaluarManual(clase(1, 24), "profe1").resultado());
    }

    @Test
    void rn09_claseQueYaEmpezoNoSeEvalua() {
        assertThrows(ClaseNoEvaluableException.class, () -> servicio.evaluarManual(clase(1, -1), "profe1"));
    }

    @Test
    void rn09_soloSeEvaluanLosEstadosProgramadaReprogramadaYEnRiesgo() {
        for (EstadoClase estado : List.of(EstadoClase.SUSPENDIDA, EstadoClase.SUSPENDIDA_SIN_FECHA, EstadoClase.CANCELADA)) {
            Clase c = clase(1, 6);
            c.setEstado(estado);
            assertThrows(ClaseNoEvaluableException.class, () -> servicio.evaluarManual(c, "profe1"));
        }
        for (EstadoClase estado : List.of(EstadoClase.PROGRAMADA, EstadoClase.REPROGRAMADA, EstadoClase.EN_RIESGO)) {
            Clase c = clase(1, 6);
            c.setEstado(estado);
            assertEquals(ResultadoEvaluacion.APTA, servicio.evaluarManual(c, "profe1").resultado());
        }
    }

    // ---------- RN11 ----------

    @Test
    void rn11_aptaNoCambiaNada() {
        clima.devolver(ResultadoEvaluacion.APTA);
        EvaluacionRespuesta r = servicio.evaluarManual(clase(1, 6), "profe1");
        assertEquals(ResultadoEvaluacion.APTA, r.resultado());
        assertEquals(EstadoClase.PROGRAMADA, r.estadoClase());
        verify(cambioEstado, never()).cambiarEstado(any(), any(), anyString(), anyString());
        verify(aviso, never()).avisar(any(), any(), anyString(), anyList());
    }

    @Test
    void rn11_enRiesgoPasaAEnRiesgoYAlertaAlProfesor() {
        clima.devolver(ResultadoEvaluacion.EN_RIESGO);
        Clase c = clase(1, 6);
        when(cambioEstado.cambiarEstado(eq(c), eq(EstadoClase.EN_RIESGO), anyString(), eq("profe1")))
                .thenReturn(cambio(10, 1, EstadoClase.PROGRAMADA, EstadoClase.EN_RIESGO));

        servicio.evaluarManual(c, "profe1");

        ArgumentCaptor<List<String>> destinatarios = ArgumentCaptor.captor();
        verify(aviso).avisar(any(), eq(TipoEvento.ALERTA_RIESGO), anyString(), destinatarios.capture());
        assertEquals(List.of("profe1"), destinatarios.getValue());
    }

    @Test
    void rn11_siYaEstabaEnRiesgoNoSeVuelveACambiarNiAAlertar() {
        clima.devolver(ResultadoEvaluacion.EN_RIESGO);
        Clase c = clase(1, 6);
        c.setEstado(EstadoClase.EN_RIESGO);
        servicio.evaluarManual(c, "SISTEMA");
        verify(cambioEstado, never()).cambiarEstado(any(), any(), anyString(), anyString());
        verify(aviso, never()).avisar(any(), any(), anyString(), anyList());
    }

    @Test
    void rn11_noAptaBuscaNuevoDiaYLuegoSuspendeYReprograma() {
        clima.devolver(ResultadoEvaluacion.NO_APTA);
        Clase c = clase(1, 6);
        LocalDateTime nuevo = c.getInicio().plusDays(1);
        PlanReprogramacion plan = new PlanReprogramacion(nuevo);
        when(reprogramacion.buscarNuevoDia(c, YOGA, SARMIENTO)).thenReturn(plan);
        when(reprogramacion.suspenderYReprogramar(eq(c), eq(YOGA), eq(plan), anyString(), eq("profe1")))
                .thenReturn(new ResultadoReprogramacion(EstadoClase.REPROGRAMADA, nuevo));

        EvaluacionRespuesta r = servicio.evaluarManual(c, "profe1");

        assertEquals(ResultadoEvaluacion.NO_APTA, r.resultado());
        assertEquals(nuevo, r.nuevoInicio());
    }

    @Test
    void rn11_siNoHayDiaViableLaRespuestaNoTieneNuevoInicio() {
        clima.devolver(ResultadoEvaluacion.NO_APTA);
        Clase c = clase(1, 6);
        PlanReprogramacion sinDia = new PlanReprogramacion(null);
        when(reprogramacion.buscarNuevoDia(c, YOGA, SARMIENTO)).thenReturn(sinDia);
        when(reprogramacion.suspenderYReprogramar(eq(c), eq(YOGA), eq(sinDia), anyString(), anyString()))
                .thenReturn(new ResultadoReprogramacion(EstadoClase.SUSPENDIDA_SIN_FECHA, null));
        assertEquals(null, servicio.evaluarManual(c, "profe1").nuevoInicio());
    }

    @Test
    void rn11_seGuardaTodaEvaluacionConLosValoresDelPronostico() {
        clima.devolver(ResultadoEvaluacion.APTA);
        servicio.evaluarManual(clase(1, 6), "profe1");
        ArgumentCaptor<EvaluacionClimatica> guardada = ArgumentCaptor.forClass(EvaluacionClimatica.class);
        verify(evaluaciones).save(guardada.capture());
        assertEquals(ResultadoEvaluacion.APTA, guardada.getValue().getResultado());
        assertEquals(1L, guardada.getValue().getClaseId());
    }

    // ---------- RN16 ----------

    @Test
    void rn16_siFallaMsClimaLaEvaluacionQuedaFallidaYLaClaseNoCambia() {
        clima.lanzarServicioNoDisponible();
        Clase c = clase(1, 6);

        EvaluacionRespuesta r = servicio.evaluarManual(c, "profe1");

        assertEquals(ResultadoEvaluacion.FALLIDA, r.resultado());
        assertEquals(EstadoClase.PROGRAMADA, r.estadoClase());
        ArgumentCaptor<EvaluacionClimatica> guardada = ArgumentCaptor.forClass(EvaluacionClimatica.class);
        verify(evaluaciones).save(guardada.capture());
        assertEquals(ResultadoEvaluacion.FALLIDA, guardada.getValue().getResultado());
        verify(cambioEstado, never()).cambiarEstado(any(), any(), anyString(), anyString());
        verify(aviso, never()).avisar(any(), any(), anyString(), anyList());
    }

    @Test
    void rn16_siFallaMsActividadesLaEvaluacionQuedaFallida() {
        when(actividades.obtenerActividad(1L)).thenThrow(new ServicioNoDisponibleException("ms-actividades caído"));
        assertEquals(ResultadoEvaluacion.FALLIDA, servicio.evaluarManual(clase(1, 6), "profe1").resultado());
        verify(cambioEstado, never()).cambiarEstado(any(), any(), anyString(), anyString());
    }

    @Test
    void rn16_siFallaLaBusquedaDeNuevoDiaLaClaseNoQuedaSuspendidaAMedias() {
        clima.devolver(ResultadoEvaluacion.NO_APTA);
        Clase c = clase(1, 6);
        when(reprogramacion.buscarNuevoDia(c, YOGA, SARMIENTO)).thenThrow(new ServicioNoDisponibleException("ms-clima caído"));

        EvaluacionRespuesta r = servicio.evaluarManual(c, "profe1");

        assertEquals(ResultadoEvaluacion.FALLIDA, r.resultado());
        assertEquals(EstadoClase.PROGRAMADA, r.estadoClase());
        verify(reprogramacion, never()).suspenderYReprogramar(any(), any(), any(), anyString(), anyString());
        verify(cambioEstado, never()).cambiarEstado(any(), any(), anyString(), anyString());
    }
}
