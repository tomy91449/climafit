package ar.edu.utn.frc.climafit.notificaciones.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Cuerpo de PUT /contactos/mio (F10).
 * <p>
 * El teléfono va en formato E.164 (+ código de país + número, sin espacios), que es lo que exige
 * WhatsApp/Twilio. Ejemplo para Córdoba: +5493511234567. Puede faltar solo si el usuario rechaza
 * los avisos.
 */
public record ContactoRequest(
        @Pattern(regexp = "^\\+[1-9]\\d{7,14}$",
                message = "debe estar en formato E.164, por ejemplo +5493511234567")
        String telefono,
        @NotNull Boolean aceptaWhatsapp) {

    /** Aceptar avisos sin dar un teléfono no tiene sentido: se rechaza con 400 DATOS_INVALIDOS. */
    @JsonIgnore
    @AssertTrue(message = "para aceptar avisos por WhatsApp hay que registrar un teléfono")
    public boolean isTelefonoPresenteSiAcepta() {
        return !Boolean.TRUE.equals(aceptaWhatsapp) || (telefono != null && !telefono.isBlank());
    }
}
