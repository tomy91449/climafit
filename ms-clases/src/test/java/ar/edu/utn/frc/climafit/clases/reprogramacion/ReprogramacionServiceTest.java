package ar.edu.utn.frc.climafit.clases.reprogramacion;

import static ar.edu.utn.frc.climafit.clases.evaluacion.Datos.SARMIENTO;
import static ar.edu.utn.frc.climafit.clases.evaluacion.Datos.YOGA;
import static ar.edu.utn.frc.climafit.clases.evaluacion.Datos.cambio;
import static ar.edu.utn.frc.climafit.clases.evaluacion.Datos.clase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.climafit.clases.aviso.AvisoService;
import ar.edu.utn.frc.climafit.clases.aviso.TipoEvento;
import ar.edu.utn.frc.climafit.clases.clase.Clase;
import ar.edu.utn.frc.climafit.clases.clase.ClaseRepository;
import ar.edu.utn.frc.climafit.clases.client.clima.ClimaClientStub;
import ar.edu.utn.frc.climafit.clases.estado.CambioEstado;
import ar.edu.utn.frc.climafit.clases.estado.CambioEstadoService;
import ar.edu.utn.frc.climafit.clases.estado.EstadoClase;
import ar.edu.utn.frc.climafit.clases.evaluacion.ResultadoEvaluacion;
import ar.edu.utn.frc.climafit.clases.inscripcion.EstadoInscripcion;
import ar.edu.utn.frc.climafit.clases.inscripcion.Inscripcion;
import ar.edu.utn.frc.climafit.clases.inscripcion.InscripcionRepository;
import ar.edu.utn.frc.climafit.commons.error.ServicioNoDisponibleException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** RN13: búsqueda de nuevo día y aplicación de la reprogramación. */
class ReprogramacionServiceTest {

    private ClimaClientStub clima;
    private ClaseRepository clases;
    private InscripcionRepository inscripciones;
    private CambioEstadoService cambioEstado;
    private AvisoService aviso;
    private ReprogramacionService servicio;
    private Clase clase;

    @BeforeEach
    void preparar() {
        clima = new ClimaClientStub();
        clases = mock(ClaseRepository.class);
        inscripciones = mock(InscripcionRepository.class);
        cambioEstado = mock(CambioEstadoService.class);
        aviso = mock(AvisoService.class);
        servicio = new ReprogramacionService(clima, clases, inscripciones, cambioEstado, aviso);
        clase = clase(7, 6);
        when(inscripciones.findByClaseIdAndEstado(7L, EstadoInscripcion.ACTIVA)).thenReturn(List.of(
                new Inscripcion(7L, "alumno1", EstadoInscripcion.ACTIVA, clase.getInicio()),
                new Inscripcion(7L, "alumno2", EstadoInscripcion.ACTIVA, clase.getInicio())));
    }

    private LocalDateTime dia(int n) {
        return clase.getInicio().plusDays(n);
    }

    @Test
    void rn13_eligeElPrimerDiaApto() {
        clima.devolver(ResultadoEvaluacion.NO_APTA)
                .devolverPara(dia(3), ResultadoEvaluacion.APTA)
                .devolverPara(dia(5), ResultadoEvaluacion.APTA);
        assertEquals(dia(3), servicio.buscarNuevoDia(clase, YOGA, SARMIENTO).nuevoInicio());
    }

    @Test
    void rn13_pruebaElMismoHorarioEnLosSieteDiasSiguientes() {
        clima.devolver(ResultadoEvaluacion.NO_APTA);
        servicio.buscarNuevoDia(clase, YOGA, SARMIENTO);
        List<LocalDateTime> inicios = clima.llamadas().get(0).stream().map(v -> v.inicio()).toList();
        assertEquals(List.of(dia(1), dia(2), dia(3), dia(4), dia(5), dia(6), dia(7)), inicios);
    }

    @Test
    void rn13_evaluaTodosLosDiasCandidatosEnUnaSolaLlamada() {
        clima.devolver(ResultadoEvaluacion.NO_APTA);
        servicio.buscarNuevoDia(clase, YOGA, SARMIENTO);
        assertEquals(1, clima.llamadas().size());
    }

    @Test
    void rn13_descartaLosDiasEnQueElProfesorTieneOtraClase_rn04() {
        when(clases.existeSuperposicionProfesor("profe1", dia(1), dia(1).plusMinutes(60), 7L)).thenReturn(true);
        PlanReprogramacion plan = servicio.buscarNuevoDia(clase, YOGA, SARMIENTO);
        assertEquals(dia(2), plan.nuevoInicio());
        assertEquals(6, clima.llamadas().get(0).size());
    }

    @Test
    void rn13_siElProfesorEstaOcupadoTodosLosDiasNoConsultaElClima() {
        when(clases.existeSuperposicionProfesor(anyString(), any(), any(), anyLong())).thenReturn(true);
        assertFalse(servicio.buscarNuevoDia(clase, YOGA, SARMIENTO).hayDia());
        assertTrue(clima.llamadas().isEmpty());
    }

    @Test
    void rn13_sinDiaViableNoHayNuevoInicio() {
        clima.devolver(ResultadoEvaluacion.NO_APTA);
        assertFalse(servicio.buscarNuevoDia(clase, YOGA, SARMIENTO).hayDia());
    }

    @Test
    void rn13_unDiaEnRiesgoNoEsViable_soloSirveElApto() {
        clima.devolver(ResultadoEvaluacion.EN_RIESGO);
        assertFalse(servicio.buscarNuevoDia(clase, YOGA, SARMIENTO).hayDia());
    }

    @Test
    void rn16_siFallaMsClimaEnLaBusquedaLaExcepcionSubeYNadaCambia() {
        clima.lanzarServicioNoDisponible();
        assertThrows(ServicioNoDisponibleException.class, () -> servicio.buscarNuevoDia(clase, YOGA, SARMIENTO));
        verify(cambioEstado, org.mockito.Mockito.never()).cambiarEstado(any(), any(), anyString(), anyString());
    }

    @Test
    void rn13_conDiaViableLaClasePasaPorSuspendidaYQuedaReprogramadaConAvisos() {
        LocalDateTime original = clase.getInicio();
        // Guardamos el candidato antes: dia(n) se calcula desde clase.getInicio(), que cambia durante la operación.
        LocalDateTime nuevoInicio = dia(1);
        CambioEstado suspension = cambio(1, 7, EstadoClase.PROGRAMADA, EstadoClase.SUSPENDIDA);
        CambioEstado reprogramacion = cambio(2, 7, EstadoClase.SUSPENDIDA, EstadoClase.REPROGRAMADA);
        when(cambioEstado.cambiarEstado(eq(clase), eq(EstadoClase.SUSPENDIDA), anyString(), eq("profe1"))).thenReturn(suspension);
        when(cambioEstado.cambiarEstado(eq(clase), eq(EstadoClase.REPROGRAMADA), anyString(), eq("SISTEMA"))).thenReturn(reprogramacion);

        ResultadoReprogramacion r = servicio.suspenderYReprogramar(clase, YOGA, new PlanReprogramacion(nuevoInicio),
                "Probabilidad de lluvia 90 % (máximo 15 %)", "profe1");

        assertEquals(EstadoClase.REPROGRAMADA, r.estadoFinal());
        assertEquals(nuevoInicio, clase.getInicio());
        assertEquals(original, reprogramacion.getInicioAnterior());
        assertEquals(nuevoInicio, reprogramacion.getInicioNuevo());

        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(aviso).avisar(eq(suspension), eq(TipoEvento.SUSPENSION), anyString(), eq(List.of("alumno1", "alumno2")));
        verify(aviso).avisar(eq(reprogramacion), eq(TipoEvento.REPROGRAMACION), mensaje.capture(), eq(List.of("alumno1", "alumno2")));
        assertTrue(mensaje.getValue().contains("se reprogramó al 15/11 18:00"));
        assertTrue(mensaje.getValue().contains("probabilidad de lluvia 90 %"));
    }

    @Test
    void rn13_sinDiaViableLaClaseQuedaSuspendidaSinFechaYSeAvisaSinFecha() {
        LocalDateTime original = clase.getInicio();
        CambioEstado suspension = cambio(1, 7, EstadoClase.PROGRAMADA, EstadoClase.SUSPENDIDA);
        CambioEstado sinFecha = cambio(2, 7, EstadoClase.SUSPENDIDA, EstadoClase.SUSPENDIDA_SIN_FECHA);
        when(cambioEstado.cambiarEstado(eq(clase), eq(EstadoClase.SUSPENDIDA), anyString(), anyString())).thenReturn(suspension);
        when(cambioEstado.cambiarEstado(eq(clase), eq(EstadoClase.SUSPENDIDA_SIN_FECHA), anyString(), eq("SISTEMA"))).thenReturn(sinFecha);

        ResultadoReprogramacion r = servicio.suspenderYReprogramar(clase, YOGA, new PlanReprogramacion(null),
                "Probabilidad de lluvia 95 % (máximo 15 %)", "SISTEMA");

        assertEquals(EstadoClase.SUSPENDIDA_SIN_FECHA, r.estadoFinal());
        assertEquals(original, clase.getInicio());   // la fecha no cambia
        verify(aviso).avisar(eq(suspension), eq(TipoEvento.SUSPENSION), anyString(), anyList());
        verify(aviso).avisar(eq(sinFecha), eq(TipoEvento.SIN_FECHA), anyString(), anyList());
    }

    @Test
    void rn13_lasInscripcionesActivasNoSeTocan_siguenActivasEnLaNuevaFecha() {
        when(cambioEstado.cambiarEstado(any(), any(), anyString(), anyString()))
                .thenReturn(cambio(1, 7, EstadoClase.PROGRAMADA, EstadoClase.SUSPENDIDA));
        servicio.suspenderYReprogramar(clase, YOGA, new PlanReprogramacion(dia(1)), "motivo", "SISTEMA");
        verify(inscripciones, org.mockito.Mockito.never()).save(any());
        verify(inscripciones, org.mockito.Mockito.never()).delete(any());
    }
}
