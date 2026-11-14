package ar.edu.utn.frc.climafit.clases.client.actividades;

/**
 * PROVISORIO (Persona 3, tarea 3.3). Fase 0, punto 9: obtenerActividad y obtenerEspacio.
 * (verificarCompatibilidad, de la Persona 3, no la usa la Persona 4 y no se incluye acá.)
 *
 * @throws ar.edu.utn.frc.climafit.commons.error.ServicioNoDisponibleException si ms-actividades falla
 */
public interface ActividadesClient {

    ActividadDto obtenerActividad(Long id);

    EspacioDto obtenerEspacio(Long id);
}
