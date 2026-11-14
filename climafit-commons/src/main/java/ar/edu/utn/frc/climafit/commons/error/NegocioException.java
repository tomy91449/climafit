package ar.edu.utn.frc.climafit.commons.error;

import org.springframework.http.HttpStatus;

/**
 * Excepción base de los errores de negocio. Cada servicio la usa (o la extiende)
 * con su propio código, por ejemplo:
 * {@code throw new NegocioException(HttpStatus.CONFLICT, "CUPO_COMPLETO", "La clase no tiene lugares disponibles");}
 */
public class NegocioException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    public NegocioException(HttpStatus status, String codigo, String mensaje) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }
}
