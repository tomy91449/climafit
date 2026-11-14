package ar.edu.utn.frc.climafit.actividades.service;

import ar.edu.utn.frc.climafit.actividades.dto.CompatibilidadRequest;
import ar.edu.utn.frc.climafit.actividades.dto.CompatibilidadResponse;
import ar.edu.utn.frc.climafit.actividades.dto.EjercicioRequest;
import ar.edu.utn.frc.climafit.actividades.dto.EjercicioResponse;
import ar.edu.utn.frc.climafit.actividades.entity.Ejercicio;
import ar.edu.utn.frc.climafit.actividades.entity.TipoActividad;
import ar.edu.utn.frc.climafit.actividades.repository.EjercicioRepository;
import ar.edu.utn.frc.climafit.actividades.repository.TipoActividadRepository;
import ar.edu.utn.frc.climafit.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.climafit.commons.error.CodigosError;
import ar.edu.utn.frc.climafit.commons.error.NegocioException;
import ar.edu.utn.frc.climafit.commons.error.RecursoNoEncontradoException;
import ar.edu.utn.frc.climafit.commons.seguridad.UsuarioActual;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Ejercicios base y personalizados (F04), con las reglas RN07 (compatibilidad con la actividad)
 * y RN08 (propiedad de los ejercicios personalizados).
 */
@Service
@Transactional
public class EjercicioService {

    private final EjercicioRepository ejercicios;
    private final TipoActividadRepository actividades;
    private final UsuarioActual usuarioActual;

    public EjercicioService(EjercicioRepository ejercicios, TipoActividadRepository actividades,
                            UsuarioActual usuarioActual) {
        this.ejercicios = ejercicios;
        this.actividades = actividades;
        this.usuarioActual = usuarioActual;
    }

    /** RN08: el usuario ve los ejercicios base y solo sus propios ejercicios personalizados. */
    @Transactional(readOnly = true)
    public List<EjercicioResponse> listarVisibles(Long actividadId) {
        return ejercicios.buscarVisibles(usuarioActual.id(), actividadId).stream()
                .map(EjercicioResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public EjercicioResponse obtener(Long id) {
        Ejercicio ejercicio = buscar(id);
        verificarVisible(ejercicio);
        return EjercicioResponse.de(ejercicio);
    }

    /** El administrador registra ejercicios base; el profesor, ejercicios personalizados propios. */
    public EjercicioResponse crear(EjercicioRequest pedido) {
        String propietarioId = usuarioActual.esAdmin() ? null : usuarioActual.id();
        Ejercicio ejercicio = new Ejercicio(pedido.nombre(), pedido.descripcion(), pedido.nivel(), propietarioId,
                buscarActividades(pedido.actividadesCompatibles()));
        return EjercicioResponse.de(ejercicios.save(ejercicio));
    }

    public EjercicioResponse actualizar(Long id, EjercicioRequest pedido) {
        Ejercicio ejercicio = buscar(id);
        verificarPuedeModificar(ejercicio);
        ejercicio.actualizar(pedido.nombre(), pedido.descripcion(), pedido.nivel(),
                buscarActividades(pedido.actividadesCompatibles()));
        return EjercicioResponse.de(ejercicio);
    }

    /** Baja lógica: las rutinas que ya usan el ejercicio no se ven afectadas (RN08). */
    public void darDeBaja(Long id) {
        Ejercicio ejercicio = buscar(id);
        verificarPuedeModificar(ejercicio);
        ejercicio.darDeBaja();
    }

    /**
     * RN07: un ejercicio es compatible con una actividad si existe, está activo y fue marcado como
     * compatible con ella. La respuesta lista los que no cumplen.
     */
    @Transactional(readOnly = true)
    public CompatibilidadResponse verificarCompatibilidad(CompatibilidadRequest pedido) {
        if (!actividades.existsById(pedido.actividadId())) {
            throw new RecursoNoEncontradoException("No existe el tipo de actividad " + pedido.actividadId());
        }
        List<Long> ids = List.copyOf(new LinkedHashSet<>(pedido.ejercicioIds()));
        Map<Long, Ejercicio> encontrados = ejercicios.findAllById(ids).stream()
                .collect(Collectors.toMap(Ejercicio::getId, Function.identity()));

        List<Long> incompatibles = ids.stream()
                .filter(id -> {
                    Ejercicio ejercicio = encontrados.get(id);
                    return ejercicio == null
                            || !ejercicio.estaActivo()
                            || !ejercicio.esCompatibleCon(pedido.actividadId());
                })
                .toList();
        return new CompatibilidadResponse(incompatibles.isEmpty(), incompatibles);
    }

    /** RN08: un ejercicio personalizado ajeno se responde como inexistente (404), para no revelar que existe. */
    private void verificarVisible(Ejercicio ejercicio) {
        if (!ejercicio.esBase() && !ejercicio.esDe(usuarioActual.id())) {
            throw noEncontrado(ejercicio.getId());
        }
    }

    /**
     * RN08: los ejercicios base solo los modifica el administrador (un profesor recibe 403);
     * los personalizados, solo su dueño (cualquier otro recibe 404).
     */
    private void verificarPuedeModificar(Ejercicio ejercicio) {
        if (ejercicio.esBase()) {
            if (!usuarioActual.esAdmin()) {
                throw new AccesoDenegadoException("Solo el administrador puede modificar los ejercicios base");
            }
            return;
        }
        if (!ejercicio.esDe(usuarioActual.id())) {
            throw noEncontrado(ejercicio.getId());
        }
    }

    private List<TipoActividad> buscarActividades(List<Long> ids) {
        List<Long> distintos = List.copyOf(new LinkedHashSet<>(ids));
        List<TipoActividad> encontradas = actividades.findAllById(distintos);
        if (encontradas.size() != distintos.size()) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, CodigosError.DATOS_INVALIDOS,
                    "Alguna de las actividades compatibles no existe");
        }
        return encontradas;
    }

    private Ejercicio buscar(Long id) {
        return ejercicios.findById(id).orElseThrow(() -> noEncontrado(id));
    }

    private static RecursoNoEncontradoException noEncontrado(Long id) {
        return new RecursoNoEncontradoException("No existe el ejercicio " + id);
    }
}
