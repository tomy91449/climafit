package ar.edu.utn.frc.climafit.commons.http;

/**
 * Entrega el token de la service account {@code climafit-servicios} (rol SERVICIO) que un
 * microservicio usa para llamar a otro.
 *
 * <p>Para principiantes: una interfaz es un "contrato": dice QUÉ se puede pedir ("dame un token")
 * pero no CÓMO se consigue. La implementación real es {@link TokenServicioOAuth2} (se lo pide a
 * Keycloak). En los tests se puede reemplazar por una versión falsa sin tocar el resto del código.
 *
 * <p>{@code @FunctionalInterface}: tiene un solo método, así que también se puede escribir como
 * lambda, por ejemplo {@code () -> "token-de-prueba"}.
 */
@FunctionalInterface
public interface TokenServicio {

    /** Un access token vigente, listo para la cabecera {@code Authorization: Bearer}. */
    String obtener();
}
