package ar.edu.utn.frc.climafit.clima.config;

import ar.edu.utn.frc.climafit.clima.evaluacion.EvaluadorClimatico;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ClimaProperties.class)
public class ClimaConfig {

    /** El evaluador es una clase sin Spring (lógica pura de RN10); acá se le pasan los márgenes. */
    @Bean
    public EvaluadorClimatico evaluadorClimatico(ClimaProperties props) {
        return new EvaluadorClimatico(props.margenes());
    }
}
