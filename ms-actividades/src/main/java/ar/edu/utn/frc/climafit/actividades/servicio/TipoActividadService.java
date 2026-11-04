package ar.edu.utn.frc.climafit.actividades.servicio;

import ar.edu.utn.frc.climafit.actividades.dominio.TipoActividad;
import ar.edu.utn.frc.climafit.actividades.dto.TipoActividadRequest;
import ar.edu.utn.frc.climafit.actividades.dto.TipoActividadResponse;
import ar.edu.utn.frc.climafit.actividades.repositorio.TipoActividadRepository;
import ar.edu.utn.frc.climafit.commons.error.NegocioException;
import ar.edu.utn.frc.climafit.commons.error.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Las reglas de negocio viven en la capa de servicio, no en el controlador ni en el repositorio.
 * El controlador solo traduce HTTP <-> Java; el repositorio solo accede a datos.
 */
@Service
public class TipoActividadService {

    private final TipoActividadRepository repositorio;

    public TipoActividadService(TipoActividadRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public List<TipoActividadResponse> listarVigentes() {
        return repositorio.findByActivoTrueOrderByNombre().stream()
                .map(TipoActividadResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public TipoActividadResponse obtener(Long id) {
        return repositorio.findById(id)
                .map(TipoActividadResponse::de)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el tipo de actividad " + id));
    }

    @Transactional
    public TipoActividadResponse registrar(TipoActividadRequest request) {
        if (repositorio.existsByNombreIgnoreCase(request.nombre())) {
            throw new NegocioException("NOMBRE_DUPLICADO", "Ya existe un tipo de actividad llamado " + request.nombre());
        }
        TipoActividad nueva = new TipoActividad(request.nombre(), request.descripcion(),
                request.probLluviaMax(), request.vientoMaxKmh(), request.tempMinC());
        return TipoActividadResponse.de(repositorio.save(nueva));
    }
}
