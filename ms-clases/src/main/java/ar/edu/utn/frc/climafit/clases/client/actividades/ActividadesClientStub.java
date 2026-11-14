package ar.edu.utn.frc.climafit.clases.client.actividades;

import ar.edu.utn.frc.climafit.commons.error.RecursoNoEncontradoException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Implementación falsa (perfil stub) para avanzar y testear sin ms-actividades.
 * Devuelve los datos iniciales de la Fase 0 (punto 10) y marca como incompatible el ejercicio 3 con Yoga.
 */
@Component
@Profile("stub")
public class ActividadesClientStub implements ActividadesClient {

    private static final Map<Long, ActividadDto> ACTIVIDADES = Map.of(
            1L, new ActividadDto(1L, "Yoga", null, 20.0, 25.0, 12.0, true),
            2L, new ActividadDto(2L, "Funcional", null, 40.0, 35.0, 5.0, true),
            3L, new ActividadDto(3L, "Running", null, 60.0, 40.0, 2.0, true));

    private static final Map<Long, EspacioDto> ESPACIOS = Map.of(
            1L, new EspacioDto(1L, "Parque Sarmiento", null, -31.4290, -64.1800, true, true),
            2L, new EspacioDto(2L, "Parque de las Tejas", null, -31.4330, -64.1880, true, true),
            3L, new EspacioDto(3L, "Gimnasio Centro", null, null, null, false, true));

    @Override
    public ActividadDto obtenerActividad(Long id) {
        ActividadDto actividad = ACTIVIDADES.get(id);
        if (actividad == null) {
            throw new RecursoNoEncontradoException("La actividad " + id + " no existe");
        }
        return actividad;
    }

    @Override
    public EspacioDto obtenerEspacio(Long id) {
        EspacioDto espacio = ESPACIOS.get(id);
        if (espacio == null) {
            throw new RecursoNoEncontradoException("El espacio " + id + " no existe");
        }
        return espacio;
    }

    @Override
    public CompatibilidadDto verificarCompatibilidad(Long actividadId, List<Long> ejercicioIds) {
        List<Long> incompatibles = ejercicioIds.stream()
                .filter(ejercicioId -> actividadId == 1L && ejercicioId == 3L)
                .toList();
        return new CompatibilidadDto(incompatibles.isEmpty(), incompatibles);
    }
}
