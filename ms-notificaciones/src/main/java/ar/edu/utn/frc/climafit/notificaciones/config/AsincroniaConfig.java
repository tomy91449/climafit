package ar.edu.utn.frc.climafit.notificaciones.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Map;

/**
 * Habilita {@code @Async} (envío en segundo plano) y {@code @Scheduled} (recuperación de pendientes).
 * El pool de hilos lo arma Spring Boot y se ajusta con {@code spring.task.execution.*}.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsincroniaConfig {

    /**
     * El correlation id vive en el MDC, que es propio de cada hilo. Sin esto, los logs del envío
     * (que corre en otro hilo) saldrían sin id y no se podría seguir un aviso desde ms-clases hasta
     * Telegram. Spring Boot aplica este decorador al pool de @Async automáticamente.
     */
    @Bean
    public TaskDecorator copiarCorrelationId() {
        return tarea -> {
            Map<String, String> contexto = MDC.getCopyOfContextMap();
            return () -> {
                if (contexto != null) {
                    MDC.setContextMap(contexto);
                }
                try {
                    tarea.run();
                } finally {
                    MDC.clear();
                }
            };
        };
    }
}
