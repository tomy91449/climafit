package ar.edu.utn.frc.climafit.clases;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/** Reloj en hora de Córdoba (Fase 0, punto 7). Se inyecta para poder fijar la hora en los tests. */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("America/Argentina/Cordoba"));
    }
}
