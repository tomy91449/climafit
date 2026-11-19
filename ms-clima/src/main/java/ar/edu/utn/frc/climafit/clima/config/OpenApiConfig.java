package ar.edu.utn.frc.climafit.clima.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación OpenAPI con el botón "Authorize" para pegar el token de Keycloak.
 * El Swagger UI unificado del gateway muestra esta misma documentación.
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA = "bearer-jwt";

    @Bean
    public OpenAPI openApiClima() {
        return new OpenAPI()
                .info(new Info()
                        .title("ClimaFit · ms-clima")
                        .version("1.0.0")
                        .description("Pronóstico de Open-Meteo (o simulado en el perfil demo) y evaluación APTA, EN_RIESGO o NO_APTA."))
                .components(new Components().addSecuritySchemes(ESQUEMA, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA));
    }
}
