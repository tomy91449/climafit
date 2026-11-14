package ar.edu.utn.frc.climafit.notificaciones.dto;

import ar.edu.utn.frc.climafit.notificaciones.dominio.Contacto;

import java.time.LocalDateTime;

public record ContactoResponse(
        String usuarioId,
        String telefono,
        boolean aceptaWhatsapp,
        LocalDateTime actualizado) {

    public static ContactoResponse de(Contacto c) {
        return new ContactoResponse(c.getUsuarioId(), c.getTelefono(), c.isAceptaWhatsapp(), c.getActualizado());
    }
}
