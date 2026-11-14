package ar.edu.utn.frc.climafit.actividades.dto;

import java.util.List;

/**
 * Respuesta de POST /ejercicios/compatibilidad (RN07): {@code compatible} es true solo si todos los
 * ejercicios lo son; {@code incompatibles} lista los que no.
 */
public record CompatibilidadResponse(boolean compatible, List<Long> incompatibles) {
}
