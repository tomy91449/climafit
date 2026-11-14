package ar.edu.utn.frc.climafit.clases.estado;

import ar.edu.utn.frc.climafit.clases.clase.Clase;
import ar.edu.utn.frc.climafit.clases.clase.ClaseRepository;
import ar.edu.utn.frc.climafit.commons.error.NegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Único lugar del código que cambia el estado de una clase.
 * Así cada cambio queda siempre validado y registrado en cambio_estado (el historial).
 */
@Service
public class CambioEstadoService {

    private final CambioEstadoRepository cambios;
    private final ClaseRepository clases;
    private final Clock clock;

    public CambioEstadoService(CambioEstadoRepository cambios, ClaseRepository clases, Clock clock) {
        this.cambios = cambios;
        this.clases = clases;
        this.clock = clock;
    }

    /** Firma de la Fase 0 (punto 9): no se modifica sin acuerdo de los 4. */
    @Transactional
    public CambioEstado cambiarEstado(Clase clase, EstadoClase nuevo, String motivo, String responsable) {
        return cambiarEstado(clase, nuevo, motivo, responsable, null);
    }

    /**
     * Sobrecarga para las reprogramaciones: además de cambiar el estado, mueve el inicio de la clase y
     * guarda inicio_anterior e inicio_nuevo en el historial. La usan la reprogramación manual (Persona 3)
     * y la automática (Persona 4).
     */
    @Transactional
    public CambioEstado cambiarEstado(Clase clase, EstadoClase nuevo, String motivo, String responsable,
                                      LocalDateTime nuevoInicio) {
        validarModificable(clase); // RN06

        EstadoClase actual = clase.getEstado();
        if (!actual.permiteTransicionA(nuevo, clase.getEstadoPrevio())) {
            throw new NegocioException(HttpStatus.CONFLICT, "CLASE_NO_MODIFICABLE",
                    "La clase no puede pasar de " + actual + " a " + nuevo);
        }
        if (nuevoInicio != null && nuevo != EstadoClase.REPROGRAMADA) {
            throw new IllegalArgumentException("Solo una reprogramación puede cambiar el inicio de la clase");
        }

        CambioEstado cambio = new CambioEstado();
        cambio.setClaseId(clase.getId());
        cambio.setFecha(ahora());
        cambio.setEstadoAnterior(actual);
        cambio.setEstadoNuevo(nuevo);
        cambio.setMotivo(motivo);
        cambio.setResponsable(responsable);
        cambio.setNotificado(false);

        if (nuevo == EstadoClase.EN_RIESGO) {
            clase.setEstadoPrevio(actual); // para poder volver si el profesor decide MANTENER
        }
        if (nuevoInicio != null) {
            cambio.setInicioAnterior(clase.getInicio());
            cambio.setInicioNuevo(nuevoInicio);
            clase.setInicio(nuevoInicio);
        }
        clase.setEstado(nuevo);
        clases.save(clase);
        return cambios.save(cambio);
    }

    /** Primer registro del historial: la clase recién creada. No hay nadie a quien avisar. */
    @Transactional
    public CambioEstado registrarCreacion(Clase clase, String responsable) {
        CambioEstado cambio = new CambioEstado();
        cambio.setClaseId(clase.getId());
        cambio.setFecha(ahora());
        cambio.setEstadoAnterior(null);
        cambio.setEstadoNuevo(clase.getEstado());
        cambio.setMotivo("Clase programada");
        cambio.setResponsable(responsable);
        cambio.setNotificado(true);
        return cambios.save(cambio);
    }

    /**
     * RN06: una clase CANCELADA, o cuya hora de inicio ya pasó, no admite cambios (409 CLASE_NO_MODIFICABLE).
     * Excepción (decisión a confirmar con el equipo): una clase SUSPENDIDA o SUSPENDIDA_SIN_FECHA ya no tiene
     * una fecha vigente, así que su inicio viejo no la bloquea; si no, no se podría reagendar nunca.
     */
    public void validarModificable(Clase clase) {
        if (clase.getEstado() == EstadoClase.CANCELADA) {
            throw new NegocioException(HttpStatus.CONFLICT, "CLASE_NO_MODIFICABLE", "La clase está cancelada");
        }
        boolean sinFechaVigente = clase.getEstado() == EstadoClase.SUSPENDIDA
                || clase.getEstado() == EstadoClase.SUSPENDIDA_SIN_FECHA;
        if (!sinFechaVigente && !clase.getInicio().isAfter(ahora())) {
            throw new NegocioException(HttpStatus.CONFLICT, "CLASE_NO_MODIFICABLE", "La clase ya comenzó");
        }
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock).withNano(0);
    }
}
