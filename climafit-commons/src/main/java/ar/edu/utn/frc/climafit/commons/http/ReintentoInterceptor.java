package ar.edu.utn.frc.climafit.commons.http;

import ar.edu.utn.frc.climafit.commons.error.ServicioNoDisponibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.Set;

/**
 * Reintenta las operaciones de lectura (GET) hasta {@code reintentos} veces cuando el otro servicio
 * no responde (error de conexión o timeout) o contesta 502, 503 o 504. Las operaciones que
 * modifican (POST, PUT, PATCH, DELETE) no se reintentan, para no ejecutarlas dos veces.
 * Si se agotan los intentos, lanza {@link ServicioNoDisponibleException} (503 SERVICIO_NO_DISPONIBLE).
 * <p>
 * Tiene que ser el último interceptor del cliente: cada reintento vuelve a ejecutar el pedido real.
 *
 * <p>Para principiantes: un "interceptor" es código que se mete en el medio de cada llamada HTTP
 * saliente, justo antes de enviarla. Acá se usa para volver a intentar si el otro servicio tuvo un
 * problema pasajero (por ejemplo, se estaba reiniciando).
 *
 * <p>¿Por qué solo GET? Leer dos veces no hace daño. Pero si un POST "crear notificación" llegó y
 * solo se perdió la respuesta, reintentarlo crearía la notificación dos veces.
 */
public class ReintentoInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ReintentoInterceptor.class);
    // Respuestas que indican un problema pasajero del otro lado (vale la pena reintentar).
    private static final Set<Integer> STATUS_REINTENTABLES = Set.of(502, 503, 504);

    private final int reintentos;

    public ReintentoInterceptor(int reintentos) {
        this.reintentos = Math.max(0, reintentos); // nunca negativo
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) {
        // Las lecturas tienen 1 intento + los reintentos (1 + 2 = 3). Las escrituras, un solo intento.
        boolean esLectura = HttpMethod.GET.equals(request.getMethod()) || HttpMethod.HEAD.equals(request.getMethod());
        int intentosPermitidos = esLectura ? reintentos + 1 : 1;

        IOException ultimoError = null;
        for (int intento = 1; intento <= intentosPermitidos; intento++) {
            try {
                // Enviar el pedido de verdad.
                ClientHttpResponse respuesta = execution.execute(request, body);
                boolean quedanIntentos = intento < intentosPermitidos;
                // Respondió 502/503/504 y quedan intentos: descartar esta respuesta y probar de nuevo.
                if (quedanIntentos && STATUS_REINTENTABLES.contains(respuesta.getStatusCode().value())) {
                    log.warn("{} {} respondió {}; reintento {} de {}", request.getMethod(), request.getURI(),
                            respuesta.getStatusCode().value(), intento, reintentos);
                    respuesta.close();
                    continue;
                }
                // Cualquier otra respuesta (200, 404, 409...) se devuelve tal cual.
                return respuesta;
            } catch (IOException ex) {
                // No se pudo conectar o se pasó el tiempo de espera: anotar y, si quedan, reintentar.
                ultimoError = ex;
                if (intento < intentosPermitidos) {
                    log.warn("{} {} falló ({}); reintento {} de {}", request.getMethod(), request.getURI(),
                            ex.getMessage(), intento, reintentos);
                }
            }
        }
        // Se agotaron los intentos sin respuesta: 503 SERVICIO_NO_DISPONIBLE.
        throw new ServicioNoDisponibleException(
                "El servicio " + request.getURI().getHost() + " no responde", ultimoError);
    }
}
