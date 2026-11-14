package ar.edu.utn.frc.climafit.clases.client.clima;

import ar.edu.utn.frc.climafit.clases.evaluacion.ResultadoEvaluacion;
import ar.edu.utn.frc.climafit.commons.error.ServicioNoDisponibleException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Implementación falsa de ClimaClient (perfil stub). Se configura desde los tests: qué
 * resultado devolver por ventana, o que lance ServicioNoDisponibleException. También registra
 * las llamadas para poder comprobar que los 7 días se pidieron en UNA sola.
 */
@Component
@Profile("stub")
public class ClimaClientStub implements ClimaClient {

    private ResultadoEvaluacion porDefecto = ResultadoEvaluacion.APTA;
    private final Map<LocalDateTime, ResultadoEvaluacion> porInicio = new HashMap<>();
    private boolean caido;
    private final List<List<VentanaDto>> llamadas = new ArrayList<>();

    /** Resultado para toda ventana que no tenga uno propio. */
    public ClimaClientStub devolver(ResultadoEvaluacion resultado) {
        this.porDefecto = resultado;
        return this;
    }

    /** Resultado para la ventana que empieza en {@code inicio}. */
    public ClimaClientStub devolverPara(LocalDateTime inicio, ResultadoEvaluacion resultado) {
        porInicio.put(inicio, resultado);
        return this;
    }

    /** Hace que toda llamada lance ServicioNoDisponibleException. */
    public ClimaClientStub lanzarServicioNoDisponible() {
        this.caido = true;
        return this;
    }

    public ClimaClientStub reiniciar() {
        porDefecto = ResultadoEvaluacion.APTA;
        porInicio.clear();
        caido = false;
        llamadas.clear();
        return this;
    }

    /** Ventanas recibidas en cada llamada, en orden. */
    public List<List<VentanaDto>> llamadas() {
        return List.copyOf(llamadas);
    }

    @Override
    public List<EvaluacionClimaDto> evaluar(double latitud, double longitud, CriteriosClimaDto criterios,
                                            List<VentanaDto> ventanas) {
        llamadas.add(List.copyOf(ventanas));
        if (caido) {
            throw new ServicioNoDisponibleException("ms-clima no responde (stub)");
        }
        return ventanas.stream().map(v -> {
            ResultadoEvaluacion r = porInicio.getOrDefault(v.inicio(), porDefecto);
            return new EvaluacionClimaDto(v.inicio(), r, "Resultado simulado " + r, 0, 5, 22, false, 0.0);
        }).toList();
    }
}
