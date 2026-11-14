package ar.edu.utn.frc.climafit.commons.http;

/**
 * Entrega el token de la service account {@code climafit-servicios} (rol SERVICIO) que un
 * microservicio usa para llamar a otro.
 */
@FunctionalInterface
public interface TokenServicio {

    /** Un access token vigente, listo para la cabecera {@code Authorization: Bearer}. */
    String obtener();
}
