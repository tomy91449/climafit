package ar.edu.utn.frc.climafit.clima.openmeteo;

import ar.edu.utn.frc.climafit.clima.config.ClimaProperties;
import ar.edu.utn.frc.climafit.clima.modelo.HoraPronostico;
import ar.edu.utn.frc.climafit.commons.error.ServicioNoDisponibleException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Cliente de la Forecast API de Open-Meteo (sin API key, sin SDK: RestClient).
 * Pide los datos por hora en la zona de Córdoba, en km/h.
 */
@Component
public class OpenMeteoClient implements ProveedorOpenMeteo {

    private static final String ZONA = "America/Argentina/Cordoba";
    private static final String VARIABLES =
            "temperature_2m,precipitation_probability,precipitation,wind_speed_10m,weather_code";

    private final RestClient http;

    public OpenMeteoClient(RestClient.Builder builder, ClimaProperties props) {
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(props.timeoutConexionSeg()))
                        .build());
        fabrica.setReadTimeout(Duration.ofSeconds(props.timeoutLecturaSeg()));
        this.http = builder.baseUrl(props.openmeteoUrl()).requestFactory(fabrica).build();
    }

    @Override
    public List<HoraPronostico> pronostico(double latitud, double longitud, LocalDate desde,
                                           LocalDate hasta) {
        try {
            RespuestaOpenMeteo respuesta = http.get()
                    .uri(uri -> uri.path("/v1/forecast")
                            .queryParam("latitude", latitud)
                            .queryParam("longitude", longitud)
                            .queryParam("hourly", VARIABLES)
                            .queryParam("timezone", ZONA)
                            .queryParam("wind_speed_unit", "kmh")
                            .queryParam("start_date", desde)
                            .queryParam("end_date", hasta)
                            .build())
                    .retrieve()
                    .body(RespuestaOpenMeteo.class);
            if (respuesta == null || respuesta.hourly() == null) {
                throw new ServicioNoDisponibleException("Open-Meteo devolvió una respuesta vacía");
            }
            return ConversorPronostico.aHoras(respuesta);
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("No se pudo consultar Open-Meteo", e);
        }
    }
}
