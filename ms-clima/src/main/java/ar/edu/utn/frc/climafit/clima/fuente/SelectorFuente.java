package ar.edu.utn.frc.climafit.clima.fuente;

import ar.edu.utn.frc.climafit.clima.config.ClimaProperties;
import ar.edu.utn.frc.climafit.clima.modelo.ModoPronostico;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

/**
 * Guarda qué modo está activo y entrega la fuente que le corresponde.
 * Fuera del perfil demo solo existe la fuente REAL, así que el modo no se puede cambiar.
 */
@Component
public class SelectorFuente {

    private final Map<ModoPronostico, FuentePronostico> fuentes = new EnumMap<>(ModoPronostico.class);
    private final AtomicReference<ModoPronostico> modoActual;

    public SelectorFuente(List<FuentePronostico> disponibles, ClimaProperties props) {
        disponibles.forEach(f -> fuentes.put(f.modo(), f));
        ModoPronostico inicial = props.modoInicial();
        if (!fuentes.containsKey(inicial)) {
            inicial = ModoPronostico.REAL;
        }
        this.modoActual = new AtomicReference<>(inicial);
    }

    public FuentePronostico actual() {
        return fuentes.get(modoActual.get());
    }

    public ModoPronostico modo() {
        return modoActual.get();
    }

    public void cambiarModo(ModoPronostico nuevo) {
        if (!fuentes.containsKey(nuevo)) {
            throw new IllegalArgumentException("El modo " + nuevo + " no está disponible en este perfil");
        }
        modoActual.set(nuevo);
    }
}
