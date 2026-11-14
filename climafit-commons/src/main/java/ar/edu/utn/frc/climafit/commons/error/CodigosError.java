package ar.edu.utn.frc.climafit.commons.error;

/**
 * Códigos de error transversales, comunes a todos los servicios.
 * Los códigos de negocio (CUPO_COMPLETO, EJERCICIO_INCOMPATIBLE, etc.) los define cada servicio.
 */
public final class CodigosError {

    public static final String DATOS_INVALIDOS = "DATOS_INVALIDOS";
    public static final String NO_AUTENTICADO = "NO_AUTENTICADO";
    public static final String ACCESO_DENEGADO = "ACCESO_DENEGADO";
    public static final String NO_ENCONTRADO = "NO_ENCONTRADO";
    public static final String SERVICIO_NO_DISPONIBLE = "SERVICIO_NO_DISPONIBLE";
    public static final String ERROR_INTERNO = "ERROR_INTERNO";

    private CodigosError() {
    }
}
