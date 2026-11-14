package ar.edu.utn.frc.climafit.clima.api;

import ar.edu.utn.frc.climafit.clima.evaluacion.EvaluacionClimaService;
import ar.edu.utn.frc.climafit.clima.modelo.HoraPronostico;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clima")
@Validated
public class PronosticoController {

    public record HoraDto(LocalDateTime fechaHora, int probLluvia, int vientoKmh, int temperaturaC,
                          boolean tormenta, double precipitacionMm) {
        static HoraDto de(HoraPronostico h) {
            return new HoraDto(h.fechaHora(), h.probLluvia(), h.vientoKmh(), h.temperaturaC(),
                    h.tormenta(), h.precipitacionMm());
        }
    }

    public record PronosticoRespuesta(double latitud, double longitud, List<HoraDto> horas) {
    }

    private final EvaluacionClimaService servicio;

    public PronosticoController(EvaluacionClimaService servicio) {
        this.servicio = servicio;
    }

    /** Pronóstico horario de una ubicación (PROFESOR / ADMIN). */
    @GetMapping("/pronostico")
    @PreAuthorize("hasAnyRole('PROFESOR','ADMIN')")
    public PronosticoRespuesta pronostico(
            @RequestParam @DecimalMin("-90") @DecimalMax("90") double latitud,
            @RequestParam @DecimalMin("-180") @DecimalMax("180") double longitud,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        List<HoraDto> horas = servicio.pronostico(latitud, longitud, desde, hasta).stream()
                .map(HoraDto::de).toList();
        return new PronosticoRespuesta(latitud, longitud, horas);
    }
}
