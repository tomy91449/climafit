package ar.edu.utn.frc.climafit.clima;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.frc.climafit.clima.config.ClimaProperties;
import ar.edu.utn.frc.climafit.clima.modelo.ModoPronostico;
import ar.edu.utn.frc.climafit.clima.openmeteo.OpenMeteoClient;
import ar.edu.utn.frc.climafit.commons.error.ServicioNoDisponibleException;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

/**
 * Manejo de errores cuando Open-Meteo tarda o no se puede alcanzar. En lugar del Open-Meteo real se
 * usa un servidor HTTP local (viene con el JDK) que demora la respuesta a propósito.
 * El timeout se baja a 1 s para que el test no tarde 10 s; la lógica es la misma.
 */
class OpenMeteoClientTimeoutTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 15);

    private HttpServer servidor;

    @AfterEach
    void apagar() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    private OpenMeteoClient cliente(String url, int timeoutLecturaSeg) {
        return new OpenMeteoClient(RestClient.builder(),
                new ClimaProperties(ModoPronostico.REAL, url, 30, 2, timeoutLecturaSeg, 10, 10, 2, 0.20));
    }

    /** Servidor local que espera {@code demoraMs} antes de contestar con el código indicado. */
    private String servidorQueDemora(long demoraMs, int status) throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/v1/forecast", intercambio -> {
            try {
                Thread.sleep(demoraMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] cuerpo = "{}".getBytes(StandardCharsets.UTF_8);
            intercambio.sendResponseHeaders(status, cuerpo.length);
            intercambio.getResponseBody().write(cuerpo);
            intercambio.close();
        });
        servidor.start();
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    @Test
    void siOpenMeteoTardaMasQueElLimitePideReintentarOComprobarLaConexion() throws IOException {
        String url = servidorQueDemora(3_000, 200);

        assertThatThrownBy(() -> cliente(url, 1).pronostico(-31.43, -64.18, HOY, HOY))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .hasMessage("El servicio de clima (Open-Meteo) tardó más de 1 segundos en responder. "
                        + "Reintentá en unos minutos o comprobá tu conexión a internet.")
                .satisfies(e -> assertThat(((ServicioNoDisponibleException) e).getStatus())
                        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void siNoSePuedeConectarPideComprobarLaConexion() throws IOException {
        int puertoLibre;
        try (ServerSocket s = new ServerSocket(0)) {
            puertoLibre = s.getLocalPort(); // se cierra enseguida: nadie escucha en ese puerto
        }

        assertThatThrownBy(() -> cliente("http://127.0.0.1:" + puertoLibre, 1).pronostico(-31.43, -64.18, HOY, HOY))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .hasMessage("No se pudo conectar con el servicio de clima (Open-Meteo). "
                        + "Comprobá tu conexión a internet y reintentá.");
    }

    @Test
    void siOpenMeteoRespondeConErrorLoInforma() throws IOException {
        String url = servidorQueDemora(0, 429);

        assertThatThrownBy(() -> cliente(url, 1).pronostico(-31.43, -64.18, HOY, HOY))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .hasMessage("El servicio de clima (Open-Meteo) respondió con un error (429). Reintentá en unos minutos.");
    }
}
