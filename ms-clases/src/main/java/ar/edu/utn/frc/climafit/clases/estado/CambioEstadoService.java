package ar.edu.utn.frc.climafit.clases.estado;

import ar.edu.utn.frc.climafit.clases.clase.Clase;
import ar.edu.utn.frc.climafit.clases.clase.ClaseRepository;
import ar.edu.utn.frc.climafit.commons.error.NegocioException;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * PROVISORIO (Persona 3, tarea 3.2). Firma de la Fase 0, punto 9:
 * valida la transición, guarda estado_previo y la fila de cambio_estado, y devuelve el
 * CambioEstado. Es el único lugar que cambia el estado de una clase.
 *
 * <p>La Persona 4 lo usa sin modificarlo. Pendiente de acordar con la Persona 3: la firma no
 * recibe inicioAnterior/inicioNuevo, así que al reprogramar la Persona 4 los completa con los
 * setters del CambioEstado devuelto (ver ReprogramacionService).
 */
@Service
public class CambioEstadoService {

    private final ClaseRepository clases;
    private final CambioEstadoRepository cambios;
    private final Clock reloj;

    public CambioEstadoService(ClaseRepository clases, CambioEstadoRepository cambios, Clock reloj) {
        this.clases = clases;
        this.cambios = cambios;
        this.reloj = reloj;
    }

    public CambioEstado cambiarEstado(Clase clase, EstadoClase nuevo, String motivo, String responsable) {
        EstadoClase anterior = clase.getEstado();
        boolean vuelveAlPrevio = anterior == EstadoClase.EN_RIESGO && nuevo == clase.getEstadoPrevio();
        if (!anterior.permiteIrA(nuevo) && !vuelveAlPrevio) {
            throw new NegocioException("TRANSICION_INVALIDA",
                    "No se puede pasar una clase de " + anterior + " a " + nuevo, HttpStatus.CONFLICT);
        }
        if (nuevo == EstadoClase.EN_RIESGO) {
            clase.setEstadoPrevio(anterior);
        }
        clase.setEstado(nuevo);
        clases.save(clase);
        return cambios.save(new CambioEstado(clase.getId(), LocalDateTime.now(reloj), anterior, nuevo,
                motivo, responsable));
    }
}
