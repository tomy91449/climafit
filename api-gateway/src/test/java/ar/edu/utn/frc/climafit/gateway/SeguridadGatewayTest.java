package ar.edu.utn.frc.climafit.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;

/**
 * Seguridad del gateway de punta a punta (propuesta, sección 10.1): 401, 403 por rol, bloqueo de
 * /internal, enrutamiento al microservicio y 503 cuando el microservicio está caído.
 * <p>
 * Un servidor HTTP local hace de ms-actividades, ms-clases y ms-notificaciones; ms-clima apunta a un
 * puerto cerrado para simular que está caído. Keycloak se reemplaza por un decodificador de prueba:
 * el token "profesor" es un JWT con realm_access.roles = [PROFESOR], y así con cada rol.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SeguridadGatewayTest {

    private static final HttpServer MICROSERVICIO = levantarMicroservicio();
    private static final AtomicReference<String> rutaRecibida = new AtomicReference<>();
    private static final AtomicReference<String> autorizacionRecibida = new AtomicReference<>();
    private static final AtomicReference<String> correlationRecibido = new AtomicReference<>();

    @Autowired
    private WebTestClient cliente;

    @LocalServerPort
    private int puerto;

    @DynamicPropertySource
    static void rutas(DynamicPropertyRegistry registro) {
        String url = "http://127.0.0.1:" + MICROSERVICIO.getAddress().getPort();
        registro.add("MS_ACTIVIDADES_URL", () -> url);
        registro.add("MS_CLASES_URL", () -> url);
        registro.add("MS_NOTIFICACIONES_URL", () -> url);
        // Puerto 1: nadie escucha -> conexión rechazada.
        registro.add("MS_CLIMA_URL", () -> "http://127.0.0.1:1");
    }

    @TestConfiguration
    static class TokensDePrueba {

        @Bean
        ReactiveJwtDecoder jwtDecoder() {
            return token -> {
                String rol = switch (token) {
                    case "admin" -> "ADMIN";
                    case "profesor" -> "PROFESOR";
                    case "alumno" -> "ALUMNO";
                    case "servicio" -> "SERVICIO";
                    default -> null;
                };
                if (rol == null) {
                    return Mono.error(new BadJwtException("Token inválido"));
                }
                return Mono.just(Jwt.withTokenValue(token)
                        .header("alg", "RS256")
                        .subject(token + "-sub")
                        .claim("realm_access", Map.of("roles", List.of(rol, "offline_access")))
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(300))
                        .build());
            };
        }
    }

    @BeforeEach
    void limpiar() {
        rutaRecibida.set(null);
        autorizacionRecibida.set(null);
        correlationRecibido.set(null);
    }

    @AfterAll
    static void apagar() {
        MICROSERVICIO.stop(0);
    }

    // ---------- 401: autenticación ----------

    @Test
    void sinTokenResponde401EnFormatoComunConWwwAuthenticate() {
        cliente.get().uri("/api/actividades").exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals("WWW-Authenticate", "Bearer")
                .expectHeader().exists("X-Correlation-Id")
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.codigo").isEqualTo("NO_AUTENTICADO")
                .jsonPath("$.ruta").isEqualTo("/api/actividades")
                .jsonPath("$.correlationId").exists()
                .jsonPath("$.fecha").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}"));
        assertThat(rutaRecibida.get()).as("no debe llegar al microservicio").isNull();
    }

    @Test
    void conTokenInvalidoResponde401() {
        cliente.get().uri("/api/actividades").headers(h -> h.setBearerAuth("token-falsificado")).exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.codigo").isEqualTo("NO_AUTENTICADO");
    }

    @Test
    void healthEsPublico() {
        cliente.get().uri("/actuator/health").exchange().expectStatus().isOk();
    }

    // ---------- Swagger UI unificado: la documentación es pública, ejecutar no ----------

    @Test
    void swaggerUiEsPublico() {
        cliente.get().uri("/swagger-ui.html").exchange()
                .expectStatus().value(status -> assertThat(status).isNotIn(401, 403));
        cliente.get().uri("/v3/api-docs/swagger-config").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.urls[0].url").isEqualTo("/docs/actividades");
    }

    @Test
    void laDocumentacionDeCadaServicioEsPublicaYApuntaAlGateway() {
        cliente.get().uri("/docs/clases").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.servers[0].url").isEqualTo("/api");
        assertThat(rutaRecibida.get()).isEqualTo("/v3/api-docs");
        assertThat(autorizacionRecibida.get()).isNull();
    }

    @Test
    void laDocumentacionSoloSeLee() {
        cliente.post().uri("/docs/clases").exchange().expectStatus().isUnauthorized();
        assertThat(rutaRecibida.get()).as("no debe llegar al microservicio").isNull();
    }

    // ---------- 403 / permitido: reglas por rol y método (sección 07) ----------

    @ParameterizedTest(name = "{1} {2} como {0} -> {3}")
    @CsvSource({
            // ms-actividades
            "alumno,    GET,    /api/actividades,                 200",
            "alumno,    POST,   /api/actividades,                 403",
            "profesor,  PUT,    /api/espacios/1,                  403",
            "admin,     POST,   /api/espacios,                    200",
            "alumno,    GET,    /api/ejercicios?actividadId=1,    200",
            "alumno,    POST,   /api/ejercicios,                  403",
            "profesor,  POST,   /api/ejercicios,                  200",
            "profesor,  POST,   /api/ejercicios/compatibilidad,   200",
            "admin,     POST,   /api/ejercicios/compatibilidad,   403",
            // ms-clases
            "profesor,  POST,   /api/clases,                      200",
            "alumno,    POST,   /api/clases,                      403",
            "admin,     POST,   /api/clases,                      403",
            "alumno,    GET,    /api/clases,                      200",
            "alumno,    PUT,    /api/clases/1,                    403",
            "admin,     PUT,    /api/clases/1,                    200",
            "profesor,  PATCH,  /api/clases/1/reprogramacion,     200",
            "admin,     PATCH,  /api/clases/1/reprogramacion,     403",
            "admin,     PATCH,  /api/clases/1/decision-riesgo,    403",
            "alumno,    PATCH,  /api/clases/1/cancelacion,        403",
            "admin,     PATCH,  /api/clases/1/cancelacion,        200",
            "alumno,    POST,   /api/clases/1/evaluaciones,       403",
            "alumno,    GET,    /api/clases/1/historial,          403",
            "profesor,  GET,    /api/clases/1/historial,          200",
            "profesor,  POST,   /api/clases/evaluaciones,         403",
            "admin,     POST,   /api/clases/evaluaciones,         200",
            "alumno,    POST,   /api/clases/1/inscripciones,      200",
            "profesor,  POST,   /api/clases/1/inscripciones,      403",
            "alumno,    GET,    /api/clases/1/inscripciones,      403",
            "alumno,    GET,    /api/inscripciones/mias,          200",
            "profesor,  GET,    /api/inscripciones/mias,          403",
            // ms-clima (el permitido da 503 porque ms-clima está caído en este test)
            "alumno,    GET,    /api/clima/pronostico,            403",
            "profesor,  PUT,    /api/clima/modo,                  403",
            "profesor,  POST,   /api/clima/escenarios,            403",
            "profesor,  GET,    /api/clima/pronostico,            503",
            // ms-notificaciones
            "alumno,    PUT,    /api/contactos/mio,               200",
            "alumno,    GET,    /api/notificaciones?claseId=1,    403",
            "profesor,  GET,    /api/notificaciones?claseId=1,    200",
    })
    void reglasPorRolYMetodo(String token, String metodo, String ruta, int esperado) {
        var respuesta = cliente.method(HttpMethod.valueOf(metodo)).uri(ruta)
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isEqualTo(esperado);
        if (esperado == 403) {
            respuesta.expectBody().jsonPath("$.codigo").isEqualTo("ACCESO_DENEGADO");
            assertThat(rutaRecibida.get()).as("un 403 no debe llegar al microservicio").isNull();
        }
    }

    // ---------- Enrutamiento ----------

    @Test
    void enrutaSinElPrefijoApiYReenviaElTokenYElCorrelationId() {
        cliente.post().uri("/api/clases")
                .headers(h -> h.setBearerAuth("profesor"))
                .header("X-Correlation-Id", "pedido-123")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Correlation-Id", "pedido-123");

        assertThat(rutaRecibida.get()).isEqualTo("/clases");
        // El microservicio vuelve a validar el mismo token (defensa en profundidad).
        assertThat(autorizacionRecibida.get()).isEqualTo("Bearer profesor");
        assertThat(correlationRecibido.get()).isEqualTo("pedido-123");
    }

    @Test
    void unCorrelationIdConFormatoInvalidoSeReemplaza() {
        cliente.get().uri("/api/actividades")
                .headers(h -> h.setBearerAuth("alumno"))
                .header("X-Correlation-Id", "invalido con espacios <script>")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().value("X-Correlation-Id", matchesPattern("[0-9a-f-]{36}"));

        assertThat(correlationRecibido.get()).matches("[0-9a-f-]{36}");
    }

    // ---------- Trusted zone: /internal/** ----------

    @Test
    void rutaInternaDesdeAfueraResponde404AunConToken() {
        cliente.post().uri("/api/internal/notificaciones")
                .headers(h -> h.setBearerAuth("servicio"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.codigo").isEqualTo("NO_ENCONTRADO");
        assertThat(rutaRecibida.get()).isNull();
    }

    @Test
    void rutaInternaSinTokenTambienResponde404() {
        cliente.post().uri("/api/internal/notificaciones").exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void noSePuedeLlegarAInternalConPuntoPunto() {
        // /api/notificaciones/** apunta a ms-notificaciones: sin el bloqueo, "/notificaciones/../internal/..."
        // llegaría al microservicio y este lo normalizaría a /internal/notificaciones.
        for (String ruta : List.of("/api/notificaciones/../internal/notificaciones",
                "/api/notificaciones/%2e%2e/internal/notificaciones",
                "/api/notificaciones/%2E%2E/INTERNAL/notificaciones",
                "/api/notificaciones;x=1/../internal;y=2/notificaciones")) {
            cliente.post().uri(URI.create("http://localhost:" + puerto + ruta))
                    .headers(h -> h.setBearerAuth("profesor"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{}")
                    .exchange()
                    .expectStatus().isNotFound();
        }
        assertThat(rutaRecibida.get()).isNull();
    }

    @Test
    void rutasConSegmentosRelativosSeRechazan() {
        cliente.get().uri(URI.create("http://localhost:" + puerto + "/api/clases/%2e%2e/actividades"))
                .headers(h -> h.setBearerAuth("alumno"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.codigo").isEqualTo("DATOS_INVALIDOS");
        assertThat(rutaRecibida.get()).isNull();
    }

    @Test
    void unaBarraCodificadaSeRechazaEnFormatoComun() {
        cliente.get().uri(URI.create("http://localhost:" + puerto + "/api/clases/1%2Fhistorial"))
                .headers(h -> h.setBearerAuth("alumno"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.codigo").isEqualTo("DATOS_INVALIDOS");
        assertThat(rutaRecibida.get()).isNull();
    }

    // ---------- Errores del propio gateway ----------

    @Test
    void microservicioCaidoResponde503ServicioNoDisponible() {
        cliente.get().uri("/api/clima/pronostico?latitud=-31.4&longitud=-64.2")
                .headers(h -> h.setBearerAuth("profesor"))
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.codigo").isEqualTo("SERVICIO_NO_DISPONIBLE")
                .jsonPath("$.mensaje").value(containsString("ms-clima"))
                .jsonPath("$.correlationId").exists();
    }

    @Test
    void rutaInexistenteResponde404EnFormatoComun() {
        cliente.get().uri("/api/no-existe")
                .headers(h -> h.setBearerAuth("alumno"))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo("NO_ENCONTRADO")
                .jsonPath("$.ruta").isEqualTo("/api/no-existe");
    }

    private static HttpServer levantarMicroservicio() {
        try {
            HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            servidor.createContext("/", intercambio -> {
                rutaRecibida.set(intercambio.getRequestURI().getPath());
                autorizacionRecibida.set(intercambio.getRequestHeaders().getFirst("Authorization"));
                correlationRecibido.set(intercambio.getRequestHeaders().getFirst("X-Correlation-Id"));
                byte[] cuerpo = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
                intercambio.getResponseHeaders().add("Content-Type", "application/json");
                intercambio.sendResponseHeaders(200, cuerpo.length);
                try (OutputStream salida = intercambio.getResponseBody()) {
                    salida.write(cuerpo);
                }
            });
            servidor.start();
            return servidor;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
