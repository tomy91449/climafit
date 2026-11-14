package ar.edu.utn.frc.climafit.commons.config;

import ar.edu.utn.frc.climafit.commons.error.ManejadorGlobalErrores;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/** Registra el manejador global de errores en cada servicio que use la librería. */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ClimafitErroresAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ManejadorGlobalErrores manejadorGlobalErrores() {
        return new ManejadorGlobalErrores();
    }
}
