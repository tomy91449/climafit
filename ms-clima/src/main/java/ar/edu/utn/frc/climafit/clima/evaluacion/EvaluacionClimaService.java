package ar.edu.utn.frc.climafit.clima.evaluacion;

import ar.edu.utn.frc.climafit.clima.fuente.SelectorFuente;
import ar.edu.utn.frc.climafit.clima.modelo.CriteriosClimaticos;
import ar.edu.utn.frc.climafit.clima.modelo.EvaluacionVentana;
import ar.edu.utn.frc.climafit.clima.modelo.HoraPronostico;
import ar.edu.utn.frc.climafit.clima.modelo.HorasDeVentana;
import ar.edu.utn.frc.climafit.clima.modelo.VentanaHoraria;
import ar.edu.utn.frc.climafit.commons.error.NegocioException;
import ar.edu.utn.frc.climafit.commons.error.ServicioNoDisponibleException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Evalúa una o varias ventanas horarias con UNA sola consulta de pronóstico (propuesta,
 * sección 06): ms-clases manda de una vez los 7 días candidatos de la reprogramación.
 */
@Service
public class EvaluacionClimaService {

    private final SelectorFuente selector;
    private final EvaluadorClimatico evaluador;

    public EvaluacionClimaService(SelectorFuente selector, EvaluadorClimatico evaluador) {
        this.selector = selector;
        this.evaluador = evaluador;
    }

    /** @return una evaluación por ventana, en el mismo orden en que llegaron */
    public List<EvaluacionVentana> evaluar(double latitud, double longitud, CriteriosClimaticos criterios,
                                           List<VentanaHoraria> ventanas) {
        if (ventanas.isEmpty()) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "DATOS_INVALIDOS",
                    "Hace falta al menos una ventana horaria");
        }
        for (VentanaHoraria v : ventanas) {
            if (!v.fin().isAfter(v.inicio())) {
                throw new NegocioException(HttpStatus.BAD_REQUEST, "DATOS_INVALIDOS",
                    "La ventana que empieza " + v.inicio() + " termina antes de empezar");
            }
        }
        LocalDateTime desde = ventanas.stream().map(VentanaHoraria::inicio).min(Comparator.naturalOrder()).get();
        LocalDateTime hasta = ventanas.stream().map(VentanaHoraria::fin).max(Comparator.naturalOrder()).get();

        List<HoraPronostico> horas = selector.actual().obtener(latitud, longitud, desde, hasta);

        return ventanas.stream().map(v -> {
            List<HoraPronostico> delaVentana = HorasDeVentana.filtrar(horas, v.inicio(), v.fin());
            if (delaVentana.isEmpty()) {
                throw new ServicioNoDisponibleException(
                        "No hay pronóstico para la ventana que empieza " + v.inicio());
            }
            return evaluador.evaluar(v, delaVentana, criterios);
        }).toList();
    }

    /** Pronóstico horario crudo (GET /clima/pronostico). */
    public List<HoraPronostico> pronostico(double latitud, double longitud, LocalDateTime desde,
                                           LocalDateTime hasta) {
        if (!hasta.isAfter(desde)) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "DATOS_INVALIDOS",
                    "'hasta' tiene que ser posterior a 'desde'");
        }
        return selector.actual().obtener(latitud, longitud, desde, hasta);
    }
}
