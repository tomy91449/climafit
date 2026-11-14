package ar.edu.utn.frc.climafit.clases.evaluacion;

import ar.edu.utn.frc.climafit.clases.clase.Clase;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Textos de los avisos por WhatsApp (en español, con la actividad, la fecha, el motivo y la
 * nueva fecha si la hay). Ejemplo de la Fase 0: "Tu clase de Yoga del 20/11 18:00 se
 * reprogramó al 21/11 18:00: probabilidad de lluvia 90%."
 */
public final class MensajesClima {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private MensajesClima() {
    }

    public static String fecha(LocalDateTime f) {
        return FECHA.format(f);
    }

    /** El motivo viene como oración ("Viento 30 km/h ..."); dentro del mensaje va en minúscula inicial. */
    static String enMinuscula(String motivo) {
        return motivo.isEmpty() ? motivo : Character.toLowerCase(motivo.charAt(0)) + motivo.substring(1);
    }

    public static String alertaRiesgo(String actividad, Clase clase, String motivo) {
        return "Tu clase de " + actividad + " del " + fecha(clase.getInicio())
                + " quedó en riesgo por el clima: " + enMinuscula(motivo) + ". Decidí si la mantenés o la suspendés.";
    }

    public static String suspension(String actividad, LocalDateTime inicioOriginal, String motivo) {
        return "Tu clase de " + actividad + " del " + fecha(inicioOriginal) + " se suspendió por el clima: " + enMinuscula(motivo) + ".";
    }

    public static String reprogramacion(String actividad, LocalDateTime inicioOriginal, LocalDateTime nuevo,
                                 String motivo) {
        return "Tu clase de " + actividad + " del " + fecha(inicioOriginal) + " se reprogramó al "
                + fecha(nuevo) + ": " + enMinuscula(motivo) + ".";
    }

    public static String sinFecha(String actividad, LocalDateTime inicioOriginal, String motivo) {
        return "Tu clase de " + actividad + " del " + fecha(inicioOriginal)
                + " se suspendió por el clima y todavía no tiene nueva fecha: " + enMinuscula(motivo)
                + ". Te avisaremos cuando se reagende.";
    }
}
