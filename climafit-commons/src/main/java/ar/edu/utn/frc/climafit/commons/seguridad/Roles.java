package ar.edu.utn.frc.climafit.commons.seguridad;

/**
 * Roles de realm de Keycloak definidos en la propuesta (sección 10.1).
 *
 * <p>Para principiantes: son solo constantes de texto. Están acá, en un único lugar, para que el
 * API Gateway y los cuatro microservicios usen exactamente los mismos nombres. Si alguien escribiera
 * "PROFESORES" en un servicio y "PROFESOR" en otro, la seguridad fallaría sin dar error de compilación;
 * usando {@code Roles.PROFESOR} el compilador avisa si el nombre no existe.
 *
 * <p>Los mismos nombres están cargados en Keycloak (infra/keycloak/climafit-realm.json).
 */
public final class Roles {

    /** Administra el catálogo (actividades, espacios, ejercicios base) y puede cancelar cualquier clase. */
    public static final String ADMIN = "ADMIN";
    /** Crea y gestiona sus propias clases. */
    public static final String PROFESOR = "PROFESOR";
    /** Consulta clases, se inscribe y gestiona su contacto de Telegram. */
    public static final String ALUMNO = "ALUMNO";
    /** Solo lo tiene la "cuenta de servicio": la usan los microservicios para llamarse entre sí (/internal/**). */
    public static final String SERVICIO = "SERVICIO";

    // Constructor privado: esta clase no se instancia (nadie hace "new Roles()"), solo se usan sus constantes.
    private Roles() {
    }
}
