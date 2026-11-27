package ar.edu.utn.frc.climafit.commons.config;

import ar.edu.utn.frc.climafit.commons.error.ManejadorGlobalErrores;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * Registra el manejador global de errores en cada servicio que use la librería.
 *
 * <p>Para principiantes:
 * <ul>
 *   <li>{@code @AutoConfiguration}: Spring Boot la carga sola porque está listada en
 *       META-INF/spring/...AutoConfiguration.imports. El microservicio no tiene que hacer nada.</li>
 *   <li>{@code @ConditionalOnWebApplication(SERVLET)}: solo se activa en aplicaciones Spring MVC
 *       (los 4 microservicios). En el API Gateway, que es WebFlux, NO se activa: el gateway tiene su
 *       propio ManejadorErroresGateway.</li>
 *   <li>{@code @Bean}: "Spring, creá este objeto y administralo vos".</li>
 *   <li>{@code @ConditionalOnMissingBean}: solo lo crea si el servicio no definió uno propio.</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ClimafitErroresAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ManejadorGlobalErrores manejadorGlobalErrores() {
        return new ManejadorGlobalErrores();
    }
}
