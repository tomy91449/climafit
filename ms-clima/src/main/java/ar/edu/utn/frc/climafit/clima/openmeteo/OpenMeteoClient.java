package ar.edu.utn.frc.climafit.clima.openmeteo;

import ar.edu.utn.frc.climafit.clima.config.ClimaProperties;
import ar.edu.utn.frc.climafit.clima.modelo.HoraPronostico;
import ar.edu.utn.frc.climafit.commons.error.ServicioNoDisponibleException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.nio.channels.UnresolvedAddressException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

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
    private final int timeoutLecturaSeg;

    /**
     * Si Open-Meteo no termina de responder en {@code timeout-lectura-seg} segundos (10 por defecto),
     * se corta la espera y se responde 503 con un mensaje que pide reintentar o revisar la conexión.
     */
    public OpenMeteoClient(RestClient.Builder builder, ClimaProperties props) {
        this.timeoutLecturaSeg = props.timeoutLecturaSeg();
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
        } catch (RestClientResponseException e) {
            // Open-Meteo contestó, pero con un error (por ejemplo 429 por exceso de pedidos o 5xx).
            throw new ServicioNoDisponibleException("El servicio de clima (Open-Meteo) respondió con un error ("
                    + e.getStatusCode().value() + "). Reintentá en unos minutos.", e);
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException(mensajeSegunCausa(e), e);
        }
    }

    /**
     * Traduce la falla técnica a un mensaje que el usuario pueda entender y sobre el que pueda actuar.
     * Se recorre la cadena de causas porque Spring envuelve la excepción original del cliente HTTP.
     */
    String mensajeSegunCausa(Throwable e) {
        for (Throwable causa = e; causa != null; causa = causa.getCause()) {
            // Va primero: HttpConnectTimeoutException es una subclase de HttpTimeoutException.
            if (causa instanceof HttpConnectTimeoutException || causa instanceof ConnectException
                    || causa instanceof UnknownHostException || causa instanceof UnresolvedAddressException) {
                return "No se pudo conectar con el servicio de clima (Open-Meteo). "
                        + "Comprobá tu conexión a internet y reintentá.";
            }
            if (causa instanceof HttpTimeoutException || causa instanceof SocketTimeoutException
                    || causa instanceof TimeoutException) {
                return "El servicio de clima (Open-Meteo) tardó más de " + timeoutLecturaSeg
                        + " segundos en responder. Reintentá en unos minutos o comprobá tu conexión a internet.";
            }
        }
        return "No se pudo consultar el servicio de clima (Open-Meteo). "
                + "Reintentá en unos minutos o comprobá tu conexión a internet.";
    }
}
