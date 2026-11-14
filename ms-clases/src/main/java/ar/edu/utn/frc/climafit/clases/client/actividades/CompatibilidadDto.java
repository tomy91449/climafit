package ar.edu.utn.frc.climafit.clases.client.actividades;

import java.util.List;

/** Respuesta de POST /ejercicios/compatibilidad (RN07). */
public record CompatibilidadDto(boolean compatible, List<Long> incompatibles) {
}
