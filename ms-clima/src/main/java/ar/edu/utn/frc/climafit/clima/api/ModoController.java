package ar.edu.utn.frc.climafit.clima.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ar.edu.utn.frc.climafit.clima.fuente.SelectorFuente;
import ar.edu.utn.frc.climafit.clima.modelo.ModoPronostico;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Elegir la fuente del pronóstico. Solo existe en el perfil demo. */
@Tag(name = "Simulación (perfil demo)", description = "Herramientas para la demo: modo del pronóstico y escenarios")
@RestController
@RequestMapping("/clima/modo")
@Profile("demo")
public class ModoController {

    public record ModoDto(@NotNull ModoPronostico modo) {
    }

    private final SelectorFuente selector;

    public ModoController(SelectorFuente selector) {
        this.selector = selector;
    }

    @Operation(summary = "Cambiar el modo del pronóstico: REAL, SIMULADO o FALLA")
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ModoDto cambiar(@Valid @RequestBody ModoDto pedido) {
        selector.cambiarModo(pedido.modo());
        return new ModoDto(selector.modo());
    }
}
