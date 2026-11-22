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

    /**
     * Código del link de vinculación (t.me/&lt;bot&gt;?start=&lt;código&gt;). Existe solo mientras el usuario
     * aceptó avisos y todavía no tocó "Iniciar" en el bot. Es de un solo uso.
     */
    @Column(name = "codigo_vinculacion")
    private String codigoVinculacion;

    @Column(name = "codigo_vence")
    private LocalDateTime codigoVence;

    @Column(nullable = false)
    private LocalDateTime actualizado;

    protected Contacto() {
        // requerido por JPA
    }

    public Contacto(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    /** RN14: el usuario acepta o rechaza los avisos. Si rechaza, un link pendiente deja de servir. */
    public void definirConsentimiento(boolean aceptaTelegram) {
        this.aceptaTelegram = aceptaTelegram;
        if (!aceptaTelegram) {
            descartarCodigo();
        }
        this.actualizado = LocalDateTime.now();
    }

    /** Guarda el chat de Telegram (por el link de vinculación o cargado a mano) y descarta el código. */
    public void registrarChat(String telegramChatId) {
        this.telegramChatId = telegramChatId;
        descartarCodigo();
        this.actualizado = LocalDateTime.now();
    }

    /** Deja listo un código nuevo para el link de vinculación. */
    public void prepararVinculacion(String codigo, LocalDateTime vence) {
        this.codigoVinculacion = codigo;
        this.codigoVence = vence;
        this.actualizado = LocalDateTime.now();
    }

    public boolean tieneChat() {
        return telegramChatId != null && !telegramChatId.isBlank();
    }

    /** El link se muestra solo si aceptó, todavía no vinculó su chat y el código no venció. */
    public boolean tieneCodigoVigente(LocalDateTime ahora) {
        return codigoVinculacion != null && codigoVence != null && codigoVence.isAfter(ahora);
    }

    /** RN14: solo se le envía por Telegram si aceptó Y tiene su chat registrado. */
    public boolean puedeRecibirTelegram() {
        return aceptaTelegram && tieneChat();
    }

    private void descartarCodigo() {
        this.codigoVinculacion = null;
        this.codigoVence = null;
    }

    public String getUsuarioId() { return usuarioId; }
    public String getTelegramChatId() { return telegramChatId; }
    public boolean isAceptaTelegram() { return aceptaTelegram; }
    public String getCodigoVinculacion() { return codigoVinculacion; }
    public LocalDateTime getCodigoVence() { return codigoVence; }
    public LocalDateTime getActualizado() { return actualizado; }
}
