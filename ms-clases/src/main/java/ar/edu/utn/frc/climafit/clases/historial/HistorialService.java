package ar.edu.utn.frc.climafit.clases.historial;

import ar.edu.utn.frc.climafit.clases.clase.AutorizacionClase;
import ar.edu.utn.frc.climafit.clases.clase.Clase;
import ar.edu.utn.frc.climafit.clases.clase.ClaseRepository;
import ar.edu.utn.frc.climafit.clases.estado.CambioEstadoRepository;
import ar.edu.utn.frc.climafit.commons.error.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HistorialService {

    private final ClaseRepository clases;
    private final CambioEstadoRepository cambios;
    private final AutorizacionClase autorizacion;

    public HistorialService(ClaseRepository clases, CambioEstadoRepository cambios, AutorizacionClase autorizacion) {
        this.clases = clases;
        this.cambios = cambios;
        this.autorizacion = autorizacion;
    }

    /** Cambios de estado de la clase, del más viejo al más nuevo. Profesor dueño o ADMIN (RN05). */
    public List<HistorialItem> consultar(Long claseId, String usuarioSub, boolean esAdmin) {
        Clase clase = clases.findById(claseId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La clase " + claseId + " no existe"));
        autorizacion.verificarDuenioOAdmin(clase, usuarioSub, esAdmin); // RN05
        return cambios.findByClaseIdOrderByFechaAscIdAsc(claseId).stream()
                .map(HistorialItem::de)
                .toList();
    }
}
