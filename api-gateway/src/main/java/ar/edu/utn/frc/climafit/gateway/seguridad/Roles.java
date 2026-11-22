package ar.edu.utn.frc.climafit.gateway.seguridad;

/** Roles de realm de Keycloak (los mismos que {@code Roles} de climafit-commons). */
public final class Roles {

    public static final String ADMIN = "ADMIN";
    public static final String PROFESOR = "PROFESOR";
    public static final String ALUMNO = "ALUMNO";
    public static final String SERVICIO = "SERVICIO";

    private Roles() {
    }
}
