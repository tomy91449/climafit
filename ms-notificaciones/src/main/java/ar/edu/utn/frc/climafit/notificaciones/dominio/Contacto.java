package ar.edu.utn.frc.climafit.notificaciones.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Preferencias de contacto de un usuario (F10). La clave es el "sub" de Keycloak, así que no hace
 * falta un id propio: cada usuario tiene a lo sumo un contacto.
 */
@Entity
@Table(name = "contacto")
public class Contacto {

    @Id
    @Column(name = "usuario_id")
    private String usuarioId;

    /** Formato E.164, por ejemplo +5493511234567. Puede ser null si el usuario no quiere avisos. */
    private String telefono;

    @Column(name = "acepta_whatsapp", nullable = false)
    private boolean aceptaWhatsapp;

    @Column(nullable = false)
    private LocalDateTime actualizado;

    protected Contacto() {
        // requerido por JPA
    }

    public Contacto(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public void actualizar(String telefono, boolean aceptaWhatsapp) {
        this.telefono = telefono;
        this.aceptaWhatsapp = aceptaWhatsapp;
        this.actualizado = LocalDateTime.now();
    }

    /** RN14: solo se le envía WhatsApp si aceptó Y tiene teléfono registrado. */
    public boolean puedeRecibirWhatsapp() {
        return aceptaWhatsapp && telefono != null && !telefono.isBlank();
    }

    public String getUsuarioId() { return usuarioId; }
    public String getTelefono() { return telefono; }
    public boolean isAceptaWhatsapp() { return aceptaWhatsapp; }
    public LocalDateTime getActualizado() { return actualizado; }
}
