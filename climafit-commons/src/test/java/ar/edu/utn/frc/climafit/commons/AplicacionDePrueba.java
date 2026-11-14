package ar.edu.utn.frc.climafit.commons;

import ar.edu.utn.frc.climafit.commons.error.NegocioException;
import ar.edu.utn.frc.climafit.commons.seguridad.UsuarioActual;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Aplicación mínima usada solo en los tests: simula un microservicio que agrega la librería
 * como dependencia, sin configurar nada más.
 */
@SpringBootApplication
public class AplicacionDePrueba {

    public record Pedido(@NotBlank String nombre, @Min(0) @Max(100) int porcentaje) {
    }

    @RestController
    static class ControladorDePrueba {

        private final UsuarioActual usuarioActual;

        ControladorDePrueba(UsuarioActual usuarioActual) {
            this.usuarioActual = usuarioActual;
        }

        @GetMapping("/yo")
        Map<String, Object> yo() {
            return Map.of("id", usuarioActual.id(), "roles", usuarioActual.roles());
        }

        @GetMapping("/solo-admin")
        @PreAuthorize("hasRole('ADMIN')")
        Map<String, String> soloAdmin() {
            return Map.of("ok", "si");
        }

        @PostMapping("/validar")
        Pedido validar(@Valid @RequestBody Pedido pedido) {
            return pedido;
        }

        @GetMapping("/negocio")
        void negocio() {
            throw new NegocioException(HttpStatus.CONFLICT, "CUPO_COMPLETO", "La clase no tiene lugares disponibles");
        }

        @GetMapping("/internal/ping")
        Map<String, String> interno() {
            return Map.of("ok", "si");
        }
    }
}
