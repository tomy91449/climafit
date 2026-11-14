package ar.edu.utn.frc.climafit.clima.api;

import ar.edu.utn.frc.climafit.clima.evaluacion.EvaluacionClimaService;
import ar.edu.utn.frc.climafit.clima.modelo.CriteriosClimaticos;
import ar.edu.utn.frc.climafit.clima.modelo.EvaluacionVentana;
import ar.edu.utn.frc.climafit.clima.modelo.ResultadoClima;
import ar.edu.utn.frc.climafit.clima.modelo.VentanaHoraria;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Contrato interno ms-clases -> ms-clima (Fase 0, punto 9). No está expuesto en el gateway. */
@RestController
@RequestMapping("/internal")
public class EvaluacionInternaController {

    public record CriteriosDto(
            @NotNull @Min(0) @Max(100) Integer probLluviaMax,
            @NotNull @Min(0) Integer vientoMaxKmh,
            @NotNull Integer tempMinC,
            /** CAMBIO DE DOMINIO: opcional; si falta no se evalúan los mm de lluvia. */
            @DecimalMin("0.0") Double lluviaMaxMmHora) {
    }

    public record VentanaDto(@NotNull LocalDateTime inicio, @NotNull LocalDateTime fin) {
    }

    public record SolicitudEvaluacion(
            @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitud,
            @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitud,
            @NotNull @Valid CriteriosDto criterios,
            @NotEmpty List<@Valid VentanaDto> ventanas) {
    }

    public record EvaluacionDto(LocalDateTime inicio, ResultadoClima resultado, String motivo,
                                int probLluvia, int vientoKmh, int temperaturaC, boolean tormenta,
                                double precipitacionMm) {
        static EvaluacionDto de(EvaluacionVentana e) {
            return new EvaluacionDto(e.inicio(), e.resultado(), e.motivo(), e.probLluvia(),
                    e.vientoKmh(), e.temperaturaC(), e.tormenta(), e.precipitacionMm());
        }
    }

    public record RespuestaEvaluaciones(List<EvaluacionDto> evaluaciones) {
    }

    private final EvaluacionClimaService servicio;

    public EvaluacionInternaController(EvaluacionClimaService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/evaluaciones")
    @PreAuthorize("hasRole('SERVICIO')")
    public RespuestaEvaluaciones evaluar(@Valid @RequestBody SolicitudEvaluacion pedido) {
        CriteriosDto c = pedido.criterios();
        List<VentanaHoraria> ventanas = pedido.ventanas().stream()
                .map(v -> new VentanaHoraria(v.inicio(), v.fin())).toList();
        List<EvaluacionVentana> resultado = servicio.evaluar(pedido.latitud(), pedido.longitud(),
                new CriteriosClimaticos(c.probLluviaMax(), c.vientoMaxKmh(), c.tempMinC(), c.lluviaMaxMmHora()),
                ventanas);
        return new RespuestaEvaluaciones(resultado.stream().map(EvaluacionDto::de).toList());
    }
}
