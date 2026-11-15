package ar.edu.utn.frc.climafit.clima.config;

import ar.edu.utn.frc.climafit.clima.modelo.MargenesRiesgo;
import ar.edu.utn.frc.climafit.clima.modelo.ModoPronostico;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Propiedades climafit.clima.* (ver application.yml). */
@ConfigurationProperties(prefix = "climafit.clima")
public record ClimaProperties(
        @DefaultValue("REAL") ModoPronostico modoInicial,
        @DefaultValue("https://api.open-meteo.com") String openmeteoUrl,
        @DefaultValue("30") int cacheMinutos,
        @DefaultValue("2") int timeoutConexionSeg,
        @DefaultValue("10") int timeoutLecturaSeg,
        @DefaultValue("10") int margenLluviaPuntos,
        @DefaultValue("10") int margenVientoKmh,
        @DefaultValue("2") int margenTemperaturaC,
        @DefaultValue("0.20") double margenLluviaMmProporcion) {

    public MargenesRiesgo margenes() {
        return new MargenesRiesgo(margenLluviaPuntos, margenVientoKmh, margenTemperaturaC,
                margenLluviaMmProporcion);
    }
}
