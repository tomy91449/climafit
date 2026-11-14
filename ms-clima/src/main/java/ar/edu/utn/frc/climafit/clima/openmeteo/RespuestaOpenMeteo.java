package ar.edu.utn.frc.climafit.clima.openmeteo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** Solo los campos que usamos de la Forecast API. Los nombres son los del JSON de Open-Meteo. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RespuestaOpenMeteo(Hourly hourly) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Hourly(List<String> time,
                         List<Double> temperature_2m,
                         List<Integer> precipitation_probability,
                         List<Double> precipitation,
                         List<Double> wind_speed_10m,
                         List<Integer> weather_code) {
    }
}
