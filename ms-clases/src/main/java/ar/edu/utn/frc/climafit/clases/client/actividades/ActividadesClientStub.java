package ar.edu.utn.frc.climafit.clases.client.actividades;

import ar.edu.utn.frc.climafit.commons.error.RecursoNoEncontradoException;
import ar.edu.utn.frc.climafit.commons.error.ServicioNoDisponibleException;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * PROVISORIO (Persona 3, perfil stub): datos iniciales de la Fase 0, punto 10, con el CAMBIO
 * DE DOMINIO del chat del 12/11 aplicado a título de propuesta:
 * criterios más estrictos, mm de lluvia por hora y dos actividades nuevas (4 y 5).
 * Los valores de la tabla se acuerdan con el equipo (ver docs/propuesta-cambios-dominio.md).
 *
 * <p>Se puede forzar la caída del servicio para los tests de RN16.
 */
@Component
@Profile("stub")
public class ActividadesClientStub implements ActividadesClient {

    // id -> nombre, lluvia máx %, viento máx km/h, temperatura mín °C, lluvia máx mm/h
    static final Map<Long, ActividadDto> ACTIVIDADES = Map.of(
            1L, new ActividadDto(1L, "Yoga", 15, 22, 14, 0.0, true),
            2L, new ActividadDto(2L, "Funcional", 35, 30, 6, 1.0, true),
            3L, new ActividadDto(3L, "Running", 50, 35, 4, 2.5, true),
            4L, new ActividadDto(4L, "Acrobacia aérea", 10, 15, 15, 0.0, true),
            5L, new ActividadDto(5L, "Patinaje", 20, 30, 8, 0.0, true));

    static final Map<Long, EspacioDto> ESPACIOS = Map.of(
            1L, new EspacioDto(1L, "Parque Sarmiento", -31.4290, -64.1800, true),
            2L, new EspacioDto(2L, "Parque de las Tejas", -31.4330, -64.1880, true),
            // La Fase 0 no da coordenadas para el gimnasio techado: 0.0 es un relleno (nunca se evalúa).
            3L, new EspacioDto(3L, "Gimnasio Centro", 0.0, 0.0, false));

    private volatile boolean caido;

    public void lanzarServicioNoDisponible() {
        this.caido = true;
    }

    public void restablecer() {
        this.caido = false;
    }

    @Override
    public ActividadDto obtenerActividad(Long id) {
        verificarDisponible();
        ActividadDto a = ACTIVIDADES.get(id);
        if (a == null) {
            throw new RecursoNoEncontradoException("No existe la actividad " + id);
        }
        return a;
    }

    @Override
    public EspacioDto obtenerEspacio(Long id) {
        verificarDisponible();
        EspacioDto e = ESPACIOS.get(id);
        if (e == null) {
            throw new RecursoNoEncontradoException("No existe el espacio " + id);
        }
        return e;
    }

    private void verificarDisponible() {
        if (caido) {
            throw new ServicioNoDisponibleException("ms-actividades no responde (stub)");
        }
    }
}
