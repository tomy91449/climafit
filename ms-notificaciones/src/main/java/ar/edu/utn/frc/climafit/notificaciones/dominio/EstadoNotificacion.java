package ar.edu.utn.frc.climafit.notificaciones.dominio;

/** Ciclo de vida de una notificación: se crea PENDIENTE y termina en uno de los otros tres. */
public enum EstadoNotificacion {
    PENDIENTE,
    /** WhatsApp aceptó el mensaje. */
    ENVIADA,
    /** RN14: el destinatario no aceptó WhatsApp o no tiene teléfono. No se intenta enviar. */
    OMITIDA,
    /** RN16: se agotaron los reintentos. No revierte el cambio de la clase. */
    FALLIDA
}
