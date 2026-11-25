package ar.edu.utn.frc.climafit.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de arranque del API Gateway: es la clase con el método main que se ejecuta al iniciar.
 * <p>
 * {@code @SpringBootApplication} le dice a Spring Boot: "esta es la aplicación; buscá todas las clases
 * de este paquete y sus subpaquetes (seguridad, error, trazabilidad) y armalas solo". Por eso no hace
 * falta crear a mano los filtros ni las configuraciones: Spring los encuentra por sus anotaciones
 * ({@code @Component}, {@code @Configuration}).
 * <p>
 * Al arrancar, Spring Boot levanta un servidor web (Netty) en el puerto 8080 y lee
 * {@code application.yml} para saber las rutas hacia cada microservicio.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
