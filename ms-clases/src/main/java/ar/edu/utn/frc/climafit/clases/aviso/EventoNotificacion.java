package ar.edu.utn.frc.climafit.clases.aviso;

import java.util.List;

/** Cuerpo de POST /internal/notificaciones (Fase 0, punto 9). Los nombres de campo son los del contrato. */
public record EventoNotificacion(String eventoId, TipoEvento tipo, Long claseId, String profesorId,
                                 String mensaje, List<String> destinatarios) {
}
