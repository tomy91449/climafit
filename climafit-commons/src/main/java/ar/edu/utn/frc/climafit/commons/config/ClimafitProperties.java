package ar.edu.utn.frc.climafit.commons.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Propiedades {@code climafit.*} de la librería común (propuesta, sección 09).
 *
 * <pre>
 * climafit:
 *   seguridad:
 *     rutas-publicas: /v3/api-docs/**, /swagger-ui/**, /swagger-ui.html, /actuator/health/**
 *   http:
 *     timeout-conexion: 2s
 *     timeout-lectura: 5s
 *     reintentos: 2
 *     registro-cliente: climafit-servicios
 * </pre>
 */
@ConfigurationProperties(prefix = "climafit")
public class ClimafitProperties {

    private final Seguridad seguridad = new Seguridad();
    private final Http http = new Http();

    public Seguridad getSeguridad() {
        return seguridad;
    }

    public Http getHttp() {
        return http;
    }

    public static class Seguridad {

        /** Rutas que no requieren token: documentación OpenAPI y salud. */
        private List<String> rutasPublicas = new ArrayList<>(List.of(
                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/actuator/health/**", "/error"));

        public List<String> getRutasPublicas() {
            return rutasPublicas;
        }

        public void setRutasPublicas(List<String> rutasPublicas) {
            this.rutasPublicas = rutasPublicas;
        }
    }

    public static class Http {

        /** Tiempo máximo para establecer la conexión con otro servicio (propuesta: 2 s). */
        private Duration timeoutConexion = Duration.ofSeconds(2);

        /** Tiempo máximo de espera de la respuesta (propuesta: 5 s). */
        private Duration timeoutLectura = Duration.ofSeconds(5);

        /** Reintentos en operaciones de lectura (propuesta: hasta 2). */
        private int reintentos = 2;

        /** Registro OAuth2 (client credentials) con el que se obtiene el token de servicio. */
        private String registroCliente = "climafit-servicios";

        public Duration getTimeoutConexion() {
            return timeoutConexion;
        }

        public void setTimeoutConexion(Duration timeoutConexion) {
            this.timeoutConexion = timeoutConexion;
        }

        public Duration getTimeoutLectura() {
            return timeoutLectura;
        }

        public void setTimeoutLectura(Duration timeoutLectura) {
            this.timeoutLectura = timeoutLectura;
        }

        public int getReintentos() {
            return reintentos;
        }

        public void setReintentos(int reintentos) {
            this.reintentos = reintentos;
        }

        public String getRegistroCliente() {
            return registroCliente;
        }

        public void setRegistroCliente(String registroCliente) {
            this.registroCliente = registroCliente;
        }
    }
}
