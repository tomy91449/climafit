package ar.edu.utn.frc.climafit.clases.tarea;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ar.edu.utn.frc.climafit.clases.aviso.AvisoService;
import ar.edu.utn.frc.climafit.clases.evaluacion.EvaluacionGeneralService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/** Tarea 4.9: primero reenvía los avisos pendientes (RN16) y después evalúa. */
class TareaEvaluacionProgramadaTest {

    private final AvisoService avisos = mock(AvisoService.class);
    private final EvaluacionGeneralService general = mock(EvaluacionGeneralService.class);
    private final TareaEvaluacionProgramada tarea = new TareaEvaluacionProgramada(avisos, general);

    @Test
    void rn16_reenviaLosAvisosPendientesAntesDeEvaluar() {
        tarea.ejecutar();
        InOrder orden = inOrder(avisos, general);
        orden.verify(avisos).reenviarPendientes();
        orden.verify(general).ejecutar("SISTEMA");
    }

    @Test
    void rn16_siFallaElReenvioIgualSeEvaluan_lasClases() {
        doThrow(new RuntimeException("ms-notificaciones caído")).when(avisos).reenviarPendientes();
        tarea.ejecutar();
        verify(general).ejecutar("SISTEMA");
    }
}
