package ar.edu.utn.frc.climafit.notificaciones.servicio;

import ar.edu.utn.frc.climafit.commons.error.CodigosError;
import ar.edu.utn.frc.climafit.commons.error.NegocioException;
import ar.edu.utn.frc.climafit.commons.error.RecursoNoEncontradoException;
import ar.edu.utn.frc.climafit.commons.seguridad.UsuarioActual;
import ar.edu.utn.frc.climafit.notificaciones.dominio.Contacto;
import ar.edu.utn.frc.climafit.notificaciones.dto.ContactoRequest;
import ar.edu.utn.frc.climafit.notificaciones.dto.ContactoResponse;
import ar.edu.utn.frc.climafit.notificaciones.repositorio.ContactoRepository;
import ar.edu.utn.frc.climafit.notificaciones.telegram.ProveedorTelegram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * F10: cada usuario (alumno o profesor) gestiona SOLO su propio contacto. Por eso la ruta es
 * /contactos/mio y el id sale del token: no hay forma de pedir o modificar el de otro.
 * <p>
 * Vinculación con Telegram: al aceptar avisos sin chat registrado se genera un código de un solo
 * uso y la respuesta trae el link {@code t.me/<bot>?start=<código>}. Cuando el usuario toca
 * "Iniciar", el bot recibe "/start &lt;código&gt;" y {@link #vincular} guarda el chat.
 * <p>
 * Para cambiar de cuenta de Telegram, {@link #nuevaVinculacion} da un link nuevo aunque ya tenga
 * chat. El chat anterior se sigue usando hasta que llegue el /start del nuevo.
 */
@Service
public class ContactoService {

    private static final Logger log = LoggerFactory.getLogger(ContactoService.class);
    private static final SecureRandom AZAR = new SecureRandom();

    /** Resultado de procesar un "/start &lt;código&gt;" que llegó al bot. */
    public enum ResultadoVinculacion { VINCULADO, CODIGO_INVALIDO }

    /**
     * Resultado de un "/start &lt;código&gt;" y, si el usuario cambió de cuenta, el chat que tenía antes
     * (para avisarle a ese chat que dejó de recibir los avisos).
     */
    public record Vinculacion(ResultadoVinculacion resultado, String chatAnterior) {

        static Vinculacion invalida() {
            return new Vinculacion(ResultadoVinculacion.CODIGO_INVALIDO, null);
        }

        /** true si quedó vinculado un chat distinto del que tenía. */
        public boolean cambioDeCuenta(String chatNuevo) {
            return resultado == ResultadoVinculacion.VINCULADO && chatAnterior != null
                    && !chatAnterior.equals(chatNuevo);
        }
    }

    private final ContactoRepository repositorio;
    private final UsuarioActual usuarioActual;
    private final ProveedorTelegram proveedor;
    private final Duration vigenciaCodigo;

    public ContactoService(ContactoRepository repositorio, UsuarioActual usuarioActual, ProveedorTelegram proveedor,
                           @Value("${climafit.telegram.vinculacion.vigencia:PT24H}") Duration vigenciaCodigo) {
        this.repositorio = repositorio;
        this.usuarioActual = usuarioActual;
        this.proveedor = proveedor;
        this.vigenciaCodigo = vigenciaCodigo;
    }

    /** PUT idempotente: la primera vez crea el contacto, las siguientes lo actualizan. */
    @Transactional
    public ContactoResponse guardarMio(ContactoRequest request) {
        String usuarioId = usuarioActual.id();
        Contacto contacto = repositorio.findById(usuarioId).orElseGet(() -> new Contacto(usuarioId));
        contacto.definirConsentimiento(request.aceptaTelegram());
        Optional<String> usuarioBot = proveedor.usuarioBot();

        if (request.telegramChatId() != null && !request.telegramChatId().isBlank()) {
            // Carga manual del chat (pruebas y demo): no hace falta el link.
            contacto.registrarChat(request.telegramChatId());
        } else if (request.aceptaTelegram() && !contacto.tieneChat() && usuarioBot.isPresent()) {
            // Aceptó pero el bot todavía no conoce su chat: se le da un link para vincularlo.
            // Si ya tenía uno vigente se reusa, así el link no cambia cada vez que guarda.
            // Sin bot real (proveedor "log") no hay link: el chat se carga a mano.
            LocalDateTime ahora = LocalDateTime.now();
            if (!contacto.tieneCodigoVigente(ahora)) {
                contacto.prepararVinculacion(nuevoCodigo(), ahora.plus(vigenciaCodigo));
            }
        }
        return ContactoResponse.de(repositorio.save(contacto), usuarioBot);
    }

    /**
     * POST /contactos/mio/vinculacion: link nuevo para vincular (o cambiar) la cuenta de Telegram,
     * aunque ya tenga un chat. Un link anterior sin usar deja de servir.
     */
    @Transactional
    public ContactoResponse nuevaVinculacion() {
        Contacto contacto = repositorio.findById(usuarioActual.id())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Primero aceptá los avisos por Telegram en tus preferencias de contacto"));
        if (!contacto.isAceptaTelegram()) {
            throw new NegocioException(HttpStatus.CONFLICT, "AVISOS_NO_ACEPTADOS",
                    "Para vincular Telegram primero tenés que aceptar los avisos");
        }
        Optional<String> usuarioBot = proveedor.usuarioBot();
        if (usuarioBot.isEmpty()) {
            throw new NegocioException(HttpStatus.SERVICE_UNAVAILABLE, CodigosError.SERVICIO_NO_DISPONIBLE,
                    "El bot de Telegram no está disponible para generar el link. "
                            + "Se puede cargar el chat a mano con telegramChatId");
        }
        contacto.prepararVinculacion(nuevoCodigo(), LocalDateTime.now().plus(vigenciaCodigo));
        log.info("Link de vinculación nuevo para el usuario {}", contacto.getUsuarioId());
        return ContactoResponse.de(repositorio.save(contacto), usuarioBot);
    }

    @Transactional(readOnly = true)
    public ContactoResponse obtenerMio() {
        return repositorio.findById(usuarioActual.id())
                .map(c -> ContactoResponse.de(c, proveedor.usuarioBot()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Todavía no registraste tus preferencias de contacto"));
    }

    /**
     * Llamado cuando el bot recibe "/start &lt;código&gt;" desde un chat. Si el código existe y no
     * venció, guarda ese chat en el contacto (reemplaza al anterior, si había) y el código deja de servir.
     */
    @Transactional
    public Vinculacion vincular(String codigo, String chatId) {
        Optional<Contacto> encontrado = repositorio.findByCodigoVinculacion(codigo);
        if (encontrado.isEmpty() || !encontrado.get().tieneCodigoVigente(LocalDateTime.now())) {
            return Vinculacion.invalida();
        }
        Contacto contacto = encontrado.get();
        String chatAnterior = contacto.getTelegramChatId();
        contacto.registrarChat(chatId);
        repositorio.save(contacto);
        log.info("Telegram vinculado para el usuario {}", contacto.getUsuarioId());
        return new Vinculacion(ResultadoVinculacion.VINCULADO, chatAnterior);
    }

    /**
     * 24 bytes al azar en Base64 URL: 32 caracteres de [A-Za-z0-9_-], que es lo que Telegram
     * acepta en el parámetro start (hasta 64). No se puede adivinar.
     */
    private static String nuevoCodigo() {
        byte[] bytes = new byte[24];
        AZAR.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
