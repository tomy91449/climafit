package ar.edu.utn.frc.climafit.commons.config;

import ar.edu.utn.frc.climafit.commons.trazabilidad.CorrelationIdFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * Registra el filtro de correlation id como el primero de la cadena, antes que la seguridad:
 * así también los 401 y 403 llevan su identificador.
 *
 * <p>Para principiantes: igual que las otras autoconfiguraciones, Spring Boot la carga sola y solo en
 * los microservicios (SERVLET). El API Gateway tiene su propio CorrelationIdFilter para WebFlux.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ClimafitTrazabilidadAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "correlationIdFilterRegistration")
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration() {
        // FilterRegistrationBean: la forma de decirle a Spring "agregá este filtro, con esta prioridad".
        FilterRegistrationBean<CorrelationIdFilter> registro = new FilterRegistrationBean<>(new CorrelationIdFilter());
        registro.setOrder(Ordered.HIGHEST_PRECEDENCE); // el primero de todos los filtros
        registro.addUrlPatterns("/*");                 // para todas las URLs
        return registro;
    }
}
