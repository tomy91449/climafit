package ar.edu.utn.frc.climafit.notificaciones.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * El aviso de un evento a UN destinatario. La restricción única (evento_id, usuario_id) de la
 * tabla es la que garantiza RN15 aunque lleguen dos pedidos iguales al mismo tiempo.
 * <p>
 * Las transiciones de estado están acá (y no sueltas en el servicio) para que no se pueda dejar
 * la notificación en un estado inválido, por ejemplo ENVIADA sin fecha de envío.
 */
@Entity
@Table(name = "notificacion")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private EventoNotificacion evento;

    @Column(name = "usuario_id", nullable = false)
    private String usuarioId;

    /** Chat de Telegram al que se envió (foto del momento del envío). */
    @Column(name = "telegram_chat_id")
    private String telegramChatId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoNotificacion estado = EstadoNotificacion.PENDIENTE;

    @Column(nullable = false)
    private int intentos;

    @Column(name = "ultimo_error")
    private String ultimoError;

    @Column(name = "id_mensaje_proveedor")
    private String idMensajeProveedor;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    protected Notificacion() {
        // requerido por JPA
    }

    public Notificacion(EventoNotificacion evento, String usuarioId) {
        this.evento = evento;
        this.usuarioId = usuarioId;
    }

    public boolean estaPendiente() {
        return estado == EstadoNotificacion.PENDIENTE;
    }

    /** RN14: sin consentimiento o sin chat de Telegram no se intenta enviar. */
    public void omitir(String motivo) {
        this.estado = EstadoNotificacion.OMITIDA;
        this.ultimoError = motivo;
    }

    /** Se llama antes de cada intento: guarda el chat usado y cuenta el intento. */
    public void registrarIntento(String telegramChatId) {
        this.telegramChatId = telegramChatId;
        this.intentos++;
    }

    public void marcarEnviada(String idMensajeProveedor) {
        this.estado = EstadoNotificacion.ENVIADA;
        this.idMensajeProveedor = idMensajeProveedor;
        this.ultimoError = null;
        this.fechaEnvio = LocalDateTime.now();
    }

    /** Un intento falló pero todavía quedan reintentos: sigue PENDIENTE. */
    public void registrarError(String error) {
        this.ultimoError = recortar(error);
    }

    /** RN16: se agotaron los reintentos. */
    public void marcarFallida(String error) {
        this.estado = EstadoNotificacion.FALLIDA;
        this.ultimoError = recortar(error);
    }

    private static String recortar(String texto) {
        if (texto == null) {
            return null;
        }
        return texto.length() <= 500 ? texto : texto.substring(0, 500);
    }

    public Long getId() { return id; }
    public EventoNotificacion getEvento() { return evento; }
    public String getUsuarioId() { return usuarioId; }
    public String getTelegramChatId() { return telegramChatId; }
    public EstadoNotificacion getEstado() { return estado; }
    public int getIntentos() { return intentos; }
    public String getUltimoError() { return ultimoError; }
    public String getIdMensajeProveedor() { return idMensajeProveedor; }
    public LocalDateTime getFechaEnvio() { return fechaEnvio; }
}
