package ar.edu.utn.frc.climafit.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentacionGatewayConfigTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void reemplazaLosServersPorElPrefijoDelGateway() throws Exception {
        String original = """
                {"openapi":"3.1.0","info":{"title":"ClimaFit · ms-clases"},
                 "servers":[{"url":"http://ms-clases:8080","description":"Generated server url"}],
                 "paths":{"/clases":{}}}
                """;

        JsonNode resultado = objectMapper.readTree(
                DocumentacionGatewayConfig.conServidorDelGateway(original, objectMapper));

        assertThat(resultado.get("servers")).hasSize(1);
        assertThat(resultado.get("servers").get(0).get("url").asText()).isEqualTo("/api");
        assertThat(resultado.get("info").get("title").asText()).isEqualTo("ClimaFit · ms-clases");
        assertThat(resultado.get("paths").has("/clases")).isTrue();
    }

    @Test
    void siNoEsUnDocumentoJsonLoDevuelveSinCambios() {
        assertThat(DocumentacionGatewayConfig.conServidorDelGateway("no es json", objectMapper))
                .isEqualTo("no es json");
        assertThat(DocumentacionGatewayConfig.conServidorDelGateway("[1,2]", objectMapper)).isEqualTo("[1,2]");
    }
}
