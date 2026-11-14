package ar.edu.utn.frc.climafit.clima;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ar.edu.utn.frc.climafit.clima.modelo.HoraPronostico;
import ar.edu.utn.frc.climafit.clima.modelo.HorasDeVentana;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class HorasDeVentanaTest {

    private static List<HoraPronostico> dia() {
        List<HoraPronostico> horas = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            horas.add(new HoraPronostico(LocalDateTime.of(2026, 11, 20, h, 0), 0, 5, 22, false, 0));
        }
        return horas;
    }

    @Test
    void claseDeUnaHoraEnPuntoUsaSoloEseBloque() {
        List<HoraPronostico> r = HorasDeVentana.filtrar(dia(),
                LocalDateTime.of(2026, 11, 20, 18, 0), LocalDateTime.of(2026, 11, 20, 19, 0));
        assertEquals(1, r.size());
        assertEquals(18, r.get(0).fechaHora().getHour());
    }

    @Test
    void claseQueEmpiezaALasMediasUsaLosDosBloquesQueToca() {
        List<HoraPronostico> r = HorasDeVentana.filtrar(dia(),
                LocalDateTime.of(2026, 11, 20, 18, 30), LocalDateTime.of(2026, 11, 20, 19, 30));
        assertEquals(2, r.size());
        assertEquals(18, r.get(0).fechaHora().getHour());
        assertEquals(19, r.get(1).fechaHora().getHour());
    }
}
