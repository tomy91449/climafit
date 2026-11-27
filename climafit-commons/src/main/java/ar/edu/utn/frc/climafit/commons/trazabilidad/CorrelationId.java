package ar.edu.utn.frc.climafit.commons.trazabilidad;

import ar.edu.utn.frc.climafit.commons.error.ErrorResponseFactory;
import org.slf4j.MDC;

/**
 * Identificador que acompaña a un pedido por todos los servicios que participan en él
 * (cabecera {@code X-Correlation-Id}). Permite seguir un proceso distribuido en los logs.
 *
 * <p>Para principiantes: cuando un profesor cancela una clase, el pedido pasa por el gateway,
 * ms-clases y ms-notificaciones. Los tres escriben en sus logs con el mismo número, así se puede
 * buscar ese número y ver todo lo que pasó con ese pedido, de punta a punta.
 *
 * <p>Recorrido: el gateway lo genera (o lo respeta si ya venía), CorrelationIdFilter lo guarda en el
 * MDC de cada microservicio, y ClienteServicioFactory lo reenvía cuando un servicio llama a otro.
 */
public final class CorrelationId {

    /** Cabecera HTTP en la que viaja el identificador. */
    public static final String CABECERA = "X-Correlation-Id";

    /** Clave del MDC (la misma que lee {@link ErrorResponseFactory}). */
    public static final String CLAVE_MDC = ErrorResponseFactory.MDC_CORRELATION_ID;

    // Constructor privado: solo constantes y métodos estáticos.
    private CorrelationId() {
    }

    /** El correlation id del pedido que se está atendiendo, o {@code null} si no hay. */
    public static String actual() {
        return MDC.get(CLAVE_MDC);
    }
}
