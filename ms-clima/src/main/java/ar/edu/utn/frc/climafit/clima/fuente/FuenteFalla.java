package ar.edu.utn.frc.climafit.clima.fuente;

import ar.edu.utn.frc.climafit.clima.modelo.HoraPronostico;
import ar.edu.utn.frc.climafit.clima.modelo.ModoPronostico;
import ar.edu.utn.frc.climafit.commons.error.ServicioNoDisponibleException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Modo FALLA: simula la caída del proveedor para demostrar RN16 (comprobación CP6). */
@Component
@Profile("demo")
public class FuenteFalla implements FuentePronostico {

    @Override
    public ModoPronostico modo() {
        return ModoPronostico.FALLA;
    }

    @Override
    public List<HoraPronostico> obtener(double latitud, double longitud, LocalDateTime desde,
                                        LocalDateTime hasta) {
        throw new ServicioNoDisponibleException("El pronóstico no está disponible (modo FALLA)");
    }
}
