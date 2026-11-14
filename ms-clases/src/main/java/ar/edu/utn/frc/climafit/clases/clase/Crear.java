package ar.edu.utn.frc.climafit.clases.clase;

import jakarta.validation.groups.Default;

/**
 * Grupo de validación para POST /clases. Los campos obligatorios al crear (@NotNull(groups = Crear.class))
 * son opcionales en PUT, donde solo se cambia lo que viene en el cuerpo.
 */
public interface Crear extends Default {
}
