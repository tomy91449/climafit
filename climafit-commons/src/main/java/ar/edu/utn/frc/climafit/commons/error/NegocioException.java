package ar.edu.utn.frc.climafit.commons.error;

import org.springframework.http.HttpStatus;

/**
 * Excepción base para las reglas de negocio. Cada microservicio define sus propios códigos
 * (por ejemplo, ms-clases lanza {@code new NegocioException("CUPO_COMPLETO", "...", HttpStatus.CONFLICT)});
 * la librería solo aporta la clase y la traducción al formato común.
 */
public class NegocioException extends RuntimeException {

    private final String codigo;
    private final HttpStatus status;

    public NegocioException(String codigo, String mensaje, HttpStatus status) {
        super(mensaje);
        this.codigo = codigo;
        this.status = status;
    }

    /** Atajo para el caso más común: una regla de negocio que impide la operación (409). */
    public NegocioException(String codigo, String mensaje) {
        this(codigo, mensaje, HttpStatus.CONFLICT);
    }

    public String getCodigo() {
        return codigo;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
