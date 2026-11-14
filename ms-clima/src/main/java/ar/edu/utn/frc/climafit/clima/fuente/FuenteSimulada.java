package ar.edu.utn.frc.climafit.clima.fuente;

import ar.edu.utn.frc.climafit.clima.modelo.Escenario;
import ar.edu.utn.frc.climafit.clima.modelo.HoraPronostico;
import ar.edu.utn.frc.climafit.clima.modelo.HorasDeVentana;
import ar.edu.utn.frc.climafit.clima.modelo.ModoPronostico;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Modo SIMULADO: los escenarios viven en memoria. Para cada hora gana el último escenario
 * cargado que la cubra; si ninguno la cubre, devuelve un tiempo apto (decisión de Fase 0,
 * la usa la colección de Postman): 0 % de lluvia, 5 km/h, 22 °C, sin tormenta.
 */
@Component
@Profile("demo")
public class FuenteSimulada implements FuentePronostico {

    /** Dos ubicaciones son "la misma" si difieren menos de esto en grados (~110 m). */
    static final double TOLERANCIA_GRADOS = 0.001;

    private final List<Escenario> escenarios = new CopyOnWriteArrayList<>();

    @Override
    public ModoPronostico modo() {
        return ModoPronostico.SIMULADO;
    }

    public Escenario agregar(Escenario escenario) {
        escenarios.add(escenario);
        return escenario;
    }

    public void borrarTodos() {
        escenarios.clear();
    }

    public List<Escenario> listar() {
        return List.copyOf(escenarios);
    }

    @Override
    public List<HoraPronostico> obtener(double latitud, double longitud, LocalDateTime desde,
                                        LocalDateTime hasta) {
        List<HoraPronostico> horas = new ArrayList<>();
        for (LocalDateTime h = HorasDeVentana.truncarAHora(desde); h.isBefore(hasta); h = h.plusHours(1)) {
            horas.add(horaPara(latitud, longitud, h));
        }
        return horas;
    }

    private HoraPronostico horaPara(double lat, double lon, LocalDateTime bloque) {
        for (int i = escenarios.size() - 1; i >= 0; i--) {
            Escenario e = escenarios.get(i);
            if (mismaUbicacion(e, lat, lon) && HorasDeVentana.cubre(bloque, e.desde(), e.hasta())) {
                return new HoraPronostico(bloque, e.probLluvia(), e.vientoKmh(), e.temperaturaC(),
                        e.tormenta(), e.precipitacionMm());
            }
        }
        return new HoraPronostico(bloque, 0, 5, 22, false, 0.0);
    }

    private static boolean mismaUbicacion(Escenario e, double lat, double lon) {
        return Math.abs(e.latitud() - lat) < TOLERANCIA_GRADOS
                && Math.abs(e.longitud() - lon) < TOLERANCIA_GRADOS;
    }
}
