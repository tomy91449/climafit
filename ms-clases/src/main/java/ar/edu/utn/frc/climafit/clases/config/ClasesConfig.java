package ar.edu.utn.frc.climafit.clases.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * El OAuth2AuthorizedClientManager del token de servicio no se declara acá: lo crea
 * ClimafitHttpAutoConfiguration de climafit-commons.
 */
@Configuration
@EnableScheduling
public class ClasesConfig {

    /** Reloj en hora de Córdoba (propuesta, sección 08). Inyectable para fijar la hora en los tests. */
    @Bean
    public Clock reloj() {
        return Clock.system(ZoneId.of("America/Argentina/Cordoba"));
    }
}
