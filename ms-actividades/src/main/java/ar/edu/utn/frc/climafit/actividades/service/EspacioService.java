package ar.edu.utn.frc.climafit.actividades.service;

import ar.edu.utn.frc.climafit.actividades.dto.EspacioRequest;
import ar.edu.utn.frc.climafit.actividades.dto.EspacioResponse;
import ar.edu.utn.frc.climafit.actividades.entity.Espacio;
import ar.edu.utn.frc.climafit.actividades.repository.EspacioRepository;
import ar.edu.utn.frc.climafit.commons.error.CodigosError;
import ar.edu.utn.frc.climafit.commons.error.NegocioException;
import ar.edu.utn.frc.climafit.commons.error.RecursoNoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Espacios con coordenadas, al aire libre o techados (F03). */
@Service
@Transactional
public class EspacioService {

    private final EspacioRepository repositorio;

    public EspacioService(EspacioRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public List<EspacioResponse> listarDisponibles() {
        return repositorio.findByActivoTrueOrderByNombreAsc().stream().map(EspacioResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public EspacioResponse obtener(Long id) {
        return EspacioResponse.de(buscar(id));
    }

    public EspacioResponse crear(EspacioRequest pedido) {
        if (repositorio.existsByNombreIgnoreCase(pedido.nombre())) {
            throw nombreDuplicado();
        }
        Espacio espacio = new Espacio(pedido.nombre(), pedido.direccion(), pedido.latitud(), pedido.longitud(),
                pedido.alAireLibre());
        return EspacioResponse.de(repositorio.save(espacio));
    }

    public EspacioResponse actualizar(Long id, EspacioRequest pedido) {
        Espacio espacio = buscar(id);
        if (repositorio.existsByNombreIgnoreCaseAndIdNot(pedido.nombre(), id)) {
            throw nombreDuplicado();
        }
        espacio.actualizar(pedido.nombre(), pedido.direccion(), pedido.latitud(), pedido.longitud(),
                pedido.alAireLibre());
        return EspacioResponse.de(espacio);
    }

    private Espacio buscar(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el espacio " + id));
    }

    private static NegocioException nombreDuplicado() {
        return new NegocioException(HttpStatus.BAD_REQUEST, CodigosError.DATOS_INVALIDOS,
                "Ya existe un espacio con ese nombre");
    }
}
