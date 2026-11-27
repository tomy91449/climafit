package ar.edu.utn.frc.climafit.commons.http;

import ar.edu.utn.frc.climafit.commons.config.ClimafitProperties;
import ar.edu.utn.frc.climafit.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.climafit.commons.trazabilidad.CorrelationId;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Fábrica de {@link RestClient} para llamar de un microservicio a otro (propuesta, secciones 06 y 09).
 * Cada cliente que crea:
 * <ul>
 *   <li>tiene timeout de conexión (2 s) y de lectura (5 s), configurables con {@code climafit.http.*};</li>
 *   <li>reintenta hasta 2 veces las lecturas (GET);</li>
 *   <li>envía el token de servicio (client credentials), si el servicio tiene configurado el registro OAuth2;</li>
 *   <li>propaga el {@code X-Correlation-Id} del pedido actual;</li>
 *   <li>traduce "no responde" y los 5xx en {@link ServicioNoDisponibleException} (503 SERVICIO_NO_DISPONIBLE).</li>
 * </ul>
 * Los 4xx del otro servicio (por ejemplo, 404) llegan como {@code RestClientResponseException} para que
 * quien llama decida qué significan.
 *
 * <pre>
 * RestClient actividades = fabrica.crear("http://ms-actividades:8081");
 * ActividadDto actividad = actividades.get().uri("/actividades/{id}", id).retrieve().body(ActividadDto.class);
 * </pre>
 *
 * <p>Para principiantes: {@link RestClient} es la herramienta de Spring para hacer pedidos HTTP desde
 * Java (como Postman, pero desde el código). Esta clase arma un RestClient "ya preparado" con todo lo
 * que ClimaFit necesita, así ms-clases no repite esa configuración en cada cliente
 * (ActividadesClientReal, ClimaClientHttp, NotificacionesClientReal).
 *
 * <p>Cada llamada pasa por los interceptores en este orden:
 * correlation id -> token de servicio -> reintentos -> envío real.
 */
public class ClienteServicioFactory {

    // Base de Spring para construir clientes (trae, por ejemplo, el conversor de JSON).
    private final RestClient.Builder builderBase;
    // Timeouts y reintentos (de ClimafitProperties: climafit.http.*).
    private final ClimafitProperties.Http propiedades;
    // Quien entrega el token de servicio. Puede ser null si el servicio no lo configuró.
    private final TokenServicio tokenServicio;

    /**
     * @param tokenServicio puede ser {@code null}: el servicio no llama a otros con token.
     */
    public ClienteServicioFactory(RestClient.Builder builderBase, ClimafitProperties.Http propiedades,
                                  TokenServicio tokenServicio) {
        this.builderBase = builderBase;
        this.propiedades = propiedades;
        this.tokenServicio = tokenServicio;
    }

    /**
     * Crea un cliente para hablar con el servicio que está en {@code urlBase}
     * (por ejemplo "http://ms-notificaciones:8080" dentro de Docker).
     */
    public RestClient crear(String urlBase) {
        // Tiempos máximos de espera: si el otro servicio no contesta, no nos quedamos colgados.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(propiedades.getTimeoutConexion()); // para conectar: 2 s
        requestFactory.setReadTimeout(propiedades.getTimeoutLectura());     // para responder: 5 s

        // clone(): copia la base para que cada cliente tenga su propia configuración.
        return builderBase.clone()
                .baseUrl(urlBase)
                .requestFactory(requestFactory)
                // Interceptor 1: reenviar el correlation id del pedido actual, para seguirlo en los logs.
                .requestInterceptor((request, body, execution) -> {
                    String correlationId = CorrelationId.actual();
                    if (correlationId != null) {
                        request.getHeaders().set(CorrelationId.CABECERA, correlationId);
                    }
                    return execution.execute(request, body); // seguir con el próximo interceptor
                })
                // Interceptor 2: agregar el token de servicio (rol SERVICIO) que se pidió a Keycloak.
                .requestInterceptor((request, body, execution) -> {
                    if (tokenServicio != null) {
                        request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + tokenServicio.obtener());
                    }
                    return execution.execute(request, body);
                })
                // Último interceptor: cada reintento vuelve a ejecutar el pedido real.
                .requestInterceptor(new ReintentoInterceptor(propiedades.getReintentos()))
                // Si el otro servicio responde un error 5xx (su problema, no nuestro), se convierte en 503.
                .defaultStatusHandler(HttpStatusCode::is5xxServerError, (request, response) -> {
                    throw new ServicioNoDisponibleException("El servicio " + request.getURI().getHost()
                            + " respondió " + response.getStatusCode().value());
                })
                .build();
    }
}
