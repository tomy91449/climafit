package ar.edu.utn.frc.climafit.clases.client.clima;

import java.util.List;

/**
 * Cliente de ms-clima (tarea 4.5). Es un puerto: la implementación real llama por HTTP y la
 * del perfil stub se configura desde los tests.
 */
public interface ClimaClient {

    /**
     * Evalúa varias ventanas horarias de una ubicación en una sola llamada.
     *
     * @return una evaluación por ventana, en el mismo orden
     * @throws ar.edu.utn.frc.climafit.commons.error.ServicioNoDisponibleException si ms-clima o
     *         Open-Meteo no responden (RN16)
     */
    List<EvaluacionClimaDto> evaluar(double latitud, double longitud, CriteriosClimaDto criterios,
                                     List<VentanaDto> ventanas);
}
