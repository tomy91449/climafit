package ar.edu.utn.frc.climafit.clases.historial;

import ar.edu.utn.frc.climafit.clases.estado.CambioEstado;
import ar.edu.utn.frc.climafit.clases.estado.EstadoClase;

import java.time.LocalDateTime;

/** Una fila de GET /clases/{id}/historial (Fase 0, punto 8). */
public record HistorialItem(LocalDateTime fecha, EstadoClase estadoAnterior, EstadoClase estadoNuevo, String motivo,
                            String responsable, LocalDateTime inicioAnterior, LocalDateTime inicioNuevo) {

    public static HistorialItem de(CambioEstado c) {
        return new HistorialItem(c.getFecha(), c.getEstadoAnterior(), c.getEstadoNuevo(), c.getMotivo(),
                c.getResponsable(), c.getInicioAnterior(), c.getInicioNuevo());
    }
}
