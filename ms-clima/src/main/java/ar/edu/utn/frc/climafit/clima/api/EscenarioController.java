package ar.edu.utn.frc.climafit.clima.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ar.edu.utn.frc.climafit.clima.fuente.FuenteSimulada;
import ar.edu.utn.frc.climafit.clima.modelo.Escenario;
import ar.edu.utn.frc.climafit.commons.error.NegocioException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Escenarios de clima simulado. Solo existe en el perfil demo. */
@Tag(name = "Simulación (perfil demo)", description = "Herramientas para la demo: modo del pronóstico y escenarios")
@RestController
@RequestMapping("/clima/escenarios")
@Profile("demo")
public class EscenarioController {

    public record EscenarioDto(
            @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitud,
            @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitud,
            @NotNull LocalDateTime desde,
            @NotNull LocalDateTime hasta,
            @NotNull @Min(0) @Max(100) Integer probLluvia,
            @NotNull @Min(0) Integer vientoKmh,
            @NotNull Integer temperaturaC,
            @NotNull Boolean tormenta,
            /** CAMBIO DE DOMINIO: opcional, por defecto 0 mm (así la colección de Postman sigue igual). */
            @DecimalMin("0.0") Double precipitacionMm) {
    }

    private final FuenteSimulada fuente;

    public EscenarioController(FuenteSimulada fuente) {
        this.fuente = fuente;
    }

    @Operation(summary = "Cargar un escenario de clima simulado para una ubicación y un rango horario")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public EscenarioDto cargar(@Valid @RequestBody EscenarioDto e) {
        if (!e.hasta().isAfter(e.desde())) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "DATOS_INVALIDOS",
                    "'hasta' tiene que ser posterior a 'desde'");
        }
        double mm = e.precipitacionMm() == null ? 0.0 : e.precipitacionMm();
        fuente.agregar(new Escenario(e.latitud(), e.longitud(), e.desde(), e.hasta(), e.probLluvia(),
                e.vientoKmh(), e.temperaturaC(), e.tormenta(), mm));
        return new EscenarioDto(e.latitud(), e.longitud(), e.desde(), e.hasta(), e.probLluvia(),
                e.vientoKmh(), e.temperaturaC(), e.tormenta(), mm);
    }

    @Operation(summary = "Borrar todos los escenarios simulados")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void borrarTodos() {
        fuente.borrarTodos();
    }
}
