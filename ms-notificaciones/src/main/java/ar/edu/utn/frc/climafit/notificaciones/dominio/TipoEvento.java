package ar.edu.utn.frc.climafit.notificaciones.dominio;

/** Qué le pasó a la clase. Lo decide ms-clases al pedir la notificación (F14). */
public enum TipoEvento {
    /** La clase pasó a SUSPENDIDA (NO_APTA o el profesor la suspendió estando EN_RIESGO). */
    SUSPENSION,
    /** La clase tiene nueva fecha: reprogramación automática (RN13) o manual (F06). */
    REPROGRAMACION,
    /** No se encontró día viable: SUSPENDIDA_SIN_FECHA (RN13). */
    SIN_FECHA,
    /** El profesor o el admin canceló la clase (F07, RN06). */
    CANCELACION,
    /** La evaluación dio EN_RIESGO: se avisa al profesor para que decida (RN12). */
    ALERTA_RIESGO
}
