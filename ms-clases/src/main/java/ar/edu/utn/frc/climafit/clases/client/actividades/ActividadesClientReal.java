package ar.edu.utn.frc.climafit.clases.client.actividades;

import ar.edu.utn.frc.climafit.commons.error.RecursoNoEncontradoException;
import ar.edu.utn.frc.climafit.commons.error.ServicioNoDisponibleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Implementación real: llama a ms-actividades con RestClient.
 *
 * TODO (Persona 2): hoy el RestClient se arma acá con el builder de Spring, sin token de servicio, sin timeouts
 * de 2 s / 5 s y sin reintentos. Cuando climafit-commons publique su fábrica de RestClient (paquete commons.http),
 * hay que reemplazar solo la línea del constructor que arma `rest`; el resto de la clase no cambia.
 * Mientras tanto, contra ms-actividades real las llamadas devolverán 401 (falta el token de servicio).
 */
@Component
@Profile("!stub")
public class ActividadesClientReal implements ActividadesClient {

    private final RestClient rest;

    public ActividadesClientReal(RestClient.Builder builder,
                                 @Value("${climafit.clientes.actividades-url}") String baseUrl) {
        this.rest = builder.baseUrl(baseUrl).build();
    }

    @Override
    public ActividadDto obtenerActividad(Long id) {
        return llamar("la actividad " + id, () ->
                rest.get().uri("/actividades/{id}", id).retrieve().body(ActividadDto.class));
    }

    @Override
    public EspacioDto obtenerEspacio(Long id) {
        return llamar("el espacio " + id, () ->
                rest.get().uri("/espacios/{id}", id).retrieve().body(EspacioDto.class));
    }

    @Override
    public CompatibilidadDto verificarCompatibilidad(Long actividadId, List<Long> ejercicioIds) {
        return llamar("la compatibilidad de ejercicios", () ->
                rest.post().uri("/ejercicios/compatibilidad")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("actividadId", actividadId, "ejercicioIds", ejercicioIds))
                        .retrieve().body(CompatibilidadDto.class));
    }

    /** Traduce los errores de HTTP: 404 -> NO_ENCONTRADO; cualquier otro problema -> SERVICIO_NO_DISPONIBLE (503). */
    private <T> T llamar(String que, Supplier<T> llamada) {
        try {
            T resultado = llamada.get();
            if (resultado == null) {
                throw new ServicioNoDisponibleException("ms-actividades devolvió una respuesta vacía para " + que);
            }
            return resultado;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                throw new RecursoNoEncontradoException("No existe " + que);
            }
            throw new ServicioNoDisponibleException(
                    "ms-actividades respondió " + e.getStatusCode().value() + " al consultar " + que, e);
        } catch (ResourceAccessException e) {
            throw new ServicioNoDisponibleException("No se pudo conectar con ms-actividades", e);
        }
    }
}
