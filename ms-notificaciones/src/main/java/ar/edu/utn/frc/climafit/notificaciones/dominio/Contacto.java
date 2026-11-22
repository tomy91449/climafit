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

    /**
     * Chat de Telegram del usuario, por ejemplo 123456789. Existe recién cuando el usuario le mandó
     * /start al bot: un bot no puede escribirle a un número de teléfono. Puede ser null si no quiere avisos.
     */
    @Column(name = "telegram_chat_id")
    private String telegramChatId;

    @Column(name = "acepta_telegram", nullable = false)
    private boolean aceptaTelegram;

    @Column(nullable = false)
    private LocalDateTime actualizado;

    protected Contacto() {
        // requerido por JPA
    }

    public Contacto(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public void actualizar(String telegramChatId, boolean aceptaTelegram) {
        this.telegramChatId = telegramChatId;
        this.aceptaTelegram = aceptaTelegram;
        this.actualizado = LocalDateTime.now();
    }

    /** RN14: solo se le envía por Telegram si aceptó Y tiene su chat registrado. */
    public boolean puedeRecibirTelegram() {
        return aceptaTelegram && telegramChatId != null && !telegramChatId.isBlank();
    }

    public String getUsuarioId() { return usuarioId; }
    public String getTelegramChatId() { return telegramChatId; }
    public boolean isAceptaTelegram() { return aceptaTelegram; }
    public LocalDateTime getActualizado() { return actualizado; }
}
