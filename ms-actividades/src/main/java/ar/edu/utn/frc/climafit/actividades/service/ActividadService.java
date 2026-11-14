package ar.edu.utn.frc.climafit.actividades.service;

import ar.edu.utn.frc.climafit.actividades.dto.ActividadRequest;
import ar.edu.utn.frc.climafit.actividades.dto.ActividadResponse;
import ar.edu.utn.frc.climafit.actividades.entity.TipoActividad;
import ar.edu.utn.frc.climafit.actividades.repository.TipoActividadRepository;
import ar.edu.utn.frc.climafit.commons.error.CodigosError;
import ar.edu.utn.frc.climafit.commons.error.NegocioException;
import ar.edu.utn.frc.climafit.commons.error.RecursoNoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Tipos de actividad y sus criterios climáticos (F02). */
@Service
@Transactional
public class ActividadService {

    private final TipoActividadRepository repositorio;

    public ActividadService(TipoActividadRepository repositorio) {
        this.repositorio = repositorio;
    }

    /** Solo las actividades vigentes: las dadas de baja no se usan en nuevas clases. */
    @Transactional(readOnly = true)
    public List<ActividadResponse> listarVigentes() {
        return repositorio.findByActivoTrueOrderByNombreAsc().stream().map(ActividadResponse::de).toList();
    }

    /** Devuelve la actividad aunque esté dada de baja; el campo {@code activo} lo indica. */
    @Transactional(readOnly = true)
    public ActividadResponse obtener(Long id) {
        return ActividadResponse.de(buscar(id));
    }

    public ActividadResponse crear(ActividadRequest pedido) {
        if (repositorio.existsByNombreIgnoreCase(pedido.nombre())) {
            throw nombreDuplicado();
        }
        TipoActividad actividad = new TipoActividad(pedido.nombre(), pedido.descripcion(),
                pedido.probLluviaMax(), pedido.lluviaMaxMmHora(), pedido.vientoMaxKmh(), pedido.tempMinC());
        return ActividadResponse.de(repositorio.save(actividad));
    }

    public ActividadResponse actualizar(Long id, ActividadRequest pedido) {
        TipoActividad actividad = buscar(id);
        if (repositorio.existsByNombreIgnoreCaseAndIdNot(pedido.nombre(), id)) {
            throw nombreDuplicado();
        }
        actividad.actualizar(pedido.nombre(), pedido.descripcion(), pedido.probLluviaMax(),
                pedido.lluviaMaxMmHora(), pedido.vientoMaxKmh(), pedido.tempMinC());
        return ActividadResponse.de(actividad);
    }

    /** Baja lógica: la actividad deja de ofrecerse, pero las clases que ya la usan no se ven afectadas. */
    public void darDeBaja(Long id) {
        buscar(id).darDeBaja();
    }

    private TipoActividad buscar(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el tipo de actividad " + id));
    }

    private static NegocioException nombreDuplicado() {
        return new NegocioException(HttpStatus.BAD_REQUEST, CodigosError.DATOS_INVALIDOS,
                "Ya existe un tipo de actividad con ese nombre");
    }
}
