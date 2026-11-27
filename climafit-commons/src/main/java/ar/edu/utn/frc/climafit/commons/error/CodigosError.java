package ar.edu.utn.frc.climafit.commons.error;

/**
 * Códigos de error transversales, comunes a todos los servicios.
 * Los códigos de negocio (CUPO_COMPLETO, EJERCICIO_INCOMPATIBLE, etc.) los define cada servicio.
 *
 * <p>Para principiantes: el código HTTP (400, 401, 404...) dice el tipo de error en general;
 * este "código" de texto dice exactamente qué pasó, y es lo que miran Postman y los tests.
 * Están en un único lugar para que el API Gateway y los microservicios usen los mismos.
 */
public final class CodigosError {

    /** 400: el JSON vino mal armado, falta un campo obligatorio o un valor no es válido. */
    public static final String DATOS_INVALIDOS = "DATOS_INVALIDOS";
    /** 401: no vino token, o vino uno inválido o vencido. */
    public static final String NO_AUTENTICADO = "NO_AUTENTICADO";
    /** 403: el usuario está identificado pero no tiene permiso (rol o dueño del recurso). */
    public static final String ACCESO_DENEGADO = "ACCESO_DENEGADO";
    /** 404: lo pedido no existe. */
    public static final String NO_ENCONTRADO = "NO_ENCONTRADO";
    /** 503: otro servicio (por ejemplo ms-clima u Open-Meteo) no respondió. */
    public static final String SERVICIO_NO_DISPONIBLE = "SERVICIO_NO_DISPONIBLE";
    /** 500: un error que no esperábamos (un bug). El detalle va al log, no al usuario. */
    public static final String ERROR_INTERNO = "ERROR_INTERNO";

    // Constructor privado: solo se usan las constantes, nadie crea objetos de esta clase.
    private CodigosError() {
    }
}
