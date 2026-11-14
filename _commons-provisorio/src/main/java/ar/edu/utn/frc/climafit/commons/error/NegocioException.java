package ar.edu.utn.frc.climafit.commons.error;

import org.springframework.http.HttpStatus;

/** Excepción base de negocio: lleva el código del contrato y el status HTTP. */
public class NegocioException extends RuntimeException {

    private final String codigo;
    private final HttpStatus status;

    public NegocioException(String codigo, HttpStatus status, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
        this.status = status;
    }

    public NegocioException(String codigo, HttpStatus status, String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.codigo = codigo;
        this.status = status;
    }

    public String getCodigo() {
        return codigo;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
