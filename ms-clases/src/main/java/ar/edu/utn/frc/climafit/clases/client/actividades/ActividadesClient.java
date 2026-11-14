package ar.edu.utn.frc.climafit.clases.client.actividades;

import java.util.List;

/**
 * Cliente de ms-actividades. ms-clases no tiene acceso a la base de ms-actividades:
 * todo lo que necesita de ahí lo pide por esta interfaz.
 */
public interface ActividadesClient {

    /** @throws ar.edu.utn.frc.climafit.commons.error.RecursoNoEncontradoException si no existe (404) */
    ActividadDto obtenerActividad(Long id);

    /** @throws ar.edu.utn.frc.climafit.commons.error.RecursoNoEncontradoException si no existe (404) */
    EspacioDto obtenerEspacio(Long id);

    /** RN07: ¿todos estos ejercicios sirven para esta actividad? */
    CompatibilidadDto verificarCompatibilidad(Long actividadId, List<Long> ejercicioIds);
}
