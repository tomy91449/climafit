package ar.edu.utn.frc.climafit.commons.http;

import ar.edu.utn.frc.climafit.commons.config.ClimafitProperties;
import ar.edu.utn.frc.climafit.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.climafit.commons.trazabilidad.CorrelationId;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba el cliente HTTP contra un servidor HTTP real y local que simula al otro microservicio.
 */
class ClienteServicioFactoryTest {

    private HttpServer servidor;
    private String urlBase;
    private final AtomicInteger llamadas = new AtomicInteger();
    private final AtomicReference<String> autorizacionRecibida = new AtomicReference<>();
    private final AtomicReference<String> correlationRecibido = new AtomicReference<>();

    @BeforeEach
    void levantarServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/ok", intercambio -> {
            llamadas.incrementAndGet();
            autorizacionRecibida.set(intercambio.getRequestHeaders().getFirst("Authorization"));
            correlationRecibido.set(intercambio.getRequestHeaders().getFirst(CorrelationId.CABECERA));
            responder(intercambio, 200, "{\"ok\":true}");
        });
        // Falla dos veces con 503 y a la tercera responde bien.
        servidor.createContext("/inestable", intercambio -> {
            int numero = llamadas.incrementAndGet();
            responder(intercambio, numero <= 2 ? 503 : 200, "{\"intento\":" + numero + "}");
        });
        servidor.createContext("/caido", intercambio -> {
            llamadas.incrementAndGet();
            responder(intercambio, 503, "{}");
        });
        servidor.createContext("/no-existe", intercambio -> {
            llamadas.incrementAndGet();
            responder(intercambio, 404, "{}");
        });
        servidor.createContext("/lento", intercambio -> {
            llamadas.incrementAndGet();
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            responder(intercambio, 200, "{}");
        });
        servidor.start();
        urlBase = "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    @AfterEach
    void apagarServidor() {
        servidor.stop(0);
        MDC.clear();
    }

    @Test
    void enviaElTokenDeServicioYElCorrelationIdDelPedidoActual() {
        MDC.put(CorrelationId.CLAVE_MDC, "pedido-1");
        RestClient cliente = fabrica(() -> "token-de-prueba").crear(urlBase);

        cliente.get().uri("/ok").retrieve().toBodilessEntity();

        assertThat(autorizacionRecibida.get()).isEqualTo("Bearer token-de-prueba");
        assertThat(correlationRecibido.get()).isEqualTo("pedido-1");
    }

    @Test
    void sinTokenServicioConfiguradoNoEnviaAuthorization() {
        fabrica(null).crear(urlBase).get().uri("/ok").retrieve().toBodilessEntity();

        assertThat(autorizacionRecibida.get()).isNull();
    }

    @Test
    void unaLecturaSeReintentaHastaDosVeces() {
        String cuerpo = fabrica(null).crear(urlBase).get().uri("/inestable").retrieve().body(String.class);

        assertThat(cuerpo).contains("\"intento\":3");
        assertThat(llamadas.get()).isEqualTo(3);
    }

    @Test
    void siSeAgotanLosReintentosResponde503ServicioNoDisponible() {
        RestClient cliente = fabrica(null).crear(urlBase);

        assertThatThrownBy(() -> cliente.get().uri("/caido").retrieve().toBodilessEntity())
                .isInstanceOf(ServicioNoDisponibleException.class);
        assertThat(llamadas.get()).isEqualTo(3);
    }

    @Test
    void unaModificacionNoSeReintenta() {
        RestClient cliente = fabrica(null).crear(urlBase);

        assertThatThrownBy(() -> cliente.post().uri("/caido").retrieve().toBodilessEntity())
                .isInstanceOf(ServicioNoDisponibleException.class);
        assertThat(llamadas.get()).isEqualTo(1);
    }

    @Test
    void unTimeoutDeLecturaTerminaEnServicioNoDisponible() {
        ClimafitProperties.Http propiedades = new ClimafitProperties.Http();
        propiedades.setTimeoutLectura(Duration.ofMillis(200));
        propiedades.setReintentos(0);
        RestClient cliente = new ClienteServicioFactory(RestClient.builder(), propiedades, null).crear(urlBase);

        assertThatThrownBy(() -> cliente.get().uri("/lento").retrieve().toBodilessEntity())
                .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void unServicioQueNoEscuchaTerminaEnServicioNoDisponible() {
        servidor.stop(0);
        RestClient cliente = fabrica(null).crear(urlBase);

        assertThatThrownBy(() -> cliente.get().uri("/ok").retrieve().toBodilessEntity())
                .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void los4xxLleganAQuienLlamaSinReintentar() {
        RestClient cliente = fabrica(null).crear(urlBase);

        assertThatThrownBy(() -> cliente.get().uri("/no-existe").retrieve().toBodilessEntity())
                .isInstanceOf(HttpClientErrorException.NotFound.class);
        assertThat(llamadas.get()).isEqualTo(1);
    }

    private ClienteServicioFactory fabrica(TokenServicio tokenServicio) {
        return new ClienteServicioFactory(RestClient.builder(), new ClimafitProperties.Http(), tokenServicio);
    }

    private static void responder(com.sun.net.httpserver.HttpExchange intercambio, int status, String cuerpo)
            throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().add("Content-Type", "application/json");
        intercambio.sendResponseHeaders(status, bytes.length);
        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(bytes);
        }
    }
}
