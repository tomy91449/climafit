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
 */
public class ReintentoInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ReintentoInterceptor.class);
    private static final Set<Integer> STATUS_REINTENTABLES = Set.of(502, 503, 504);

    private final int reintentos;

    public ReintentoInterceptor(int reintentos) {
        this.reintentos = Math.max(0, reintentos);
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) {
        boolean esLectura = HttpMethod.GET.equals(request.getMethod()) || HttpMethod.HEAD.equals(request.getMethod());
        int intentosPermitidos = esLectura ? reintentos + 1 : 1;

        IOException ultimoError = null;
        for (int intento = 1; intento <= intentosPermitidos; intento++) {
            try {
                ClientHttpResponse respuesta = execution.execute(request, body);
                boolean quedanIntentos = intento < intentosPermitidos;
                if (quedanIntentos && STATUS_REINTENTABLES.contains(respuesta.getStatusCode().value())) {
                    log.warn("{} {} respondió {}; reintento {} de {}", request.getMethod(), request.getURI(),
                            respuesta.getStatusCode().value(), intento, reintentos);
                    respuesta.close();
                    continue;
                }
                return respuesta;
            } catch (IOException ex) {
                ultimoError = ex;
                if (intento < intentosPermitidos) {
                    log.warn("{} {} falló ({}); reintento {} de {}", request.getMethod(), request.getURI(),
                            ex.getMessage(), intento, reintentos);
                }
            }
        }
        throw new ServicioNoDisponibleException(
                "El servicio " + request.getURI().getHost() + " no responde", ultimoError);
    }
}
