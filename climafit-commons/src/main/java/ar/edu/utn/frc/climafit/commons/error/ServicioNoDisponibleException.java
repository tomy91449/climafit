package ar.edu.utn.frc.climafit.commons.error;

import org.springframework.http.HttpStatus;

/**
 * 503 SERVICIO_NO_DISPONIBLE: otro servicio (interno o externo) no respondió a tiempo o falló.
 *
 * <p>Para principiantes: la lanzan los clientes HTTP cuando, por ejemplo, ms-clases llama a ms-clima
 * y ms-clima está apagado, o cuando ms-clima no puede consultar Open-Meteo. Quien la recibe decide
 * qué hacer: en la evaluación climática (RN16) la evaluación queda FALLIDA y la clase no cambia.
 */
public class ServicioNoDisponibleException extends NegocioException {

    public ServicioNoDisponibleException(String mensaje) {
        super(HttpStatus.SERVICE_UNAVAILABLE, CodigosError.SERVICIO_NO_DISPONIBLE, mensaje);
    }

    /** Igual que el anterior, pero guarda el error original ("causa") para verlo completo en el log. */
    public ServicioNoDisponibleException(String mensaje, Throwable causa) {
        this(mensaje);
        initCause(causa);
    }
}
