package ar.edu.utn.frc.climafit.actividades.servicio;

import ar.edu.utn.frc.climafit.actividades.dominio.TipoActividad;
import ar.edu.utn.frc.climafit.actividades.dto.TipoActividadRequest;
import ar.edu.utn.frc.climafit.actividades.repositorio.TipoActividadRepository;
import ar.edu.utn.frc.climafit.commons.error.NegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Test unitario: el repositorio es un "mock", así se prueba solo la regla del servicio, sin base de datos. */
@ExtendWith(MockitoExtension.class)
class TipoActividadServiceTest {

    @Mock
    private TipoActividadRepository repositorio;

    @InjectMocks
    private TipoActividadService servicio;

    private final TipoActividadRequest pilates = new TipoActividadRequest("Pilates", "Pilates de suelo", 20, 25, 12);

    @Test
    void registraUnaActividadNueva() {
        when(repositorio.existsByNombreIgnoreCase("Pilates")).thenReturn(false);
        when(repositorio.save(any(TipoActividad.class))).thenAnswer(inv -> inv.getArgument(0));

        var creada = servicio.registrar(pilates);

        assertThat(creada.nombre()).isEqualTo("Pilates");
        assertThat(creada.vientoMaxKmh()).isEqualTo(25);
        assertThat(creada.activo()).isTrue();
    }

    @Test
    void rechazaUnNombreDuplicadoCon409() {
        when(repositorio.existsByNombreIgnoreCase("Pilates")).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(pilates))
                .isInstanceOf(NegocioException.class)
                .satisfies(ex -> {
                    NegocioException negocio = (NegocioException) ex;
                    assertThat(negocio.getCodigo()).isEqualTo("NOMBRE_DUPLICADO");
                    assertThat(negocio.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                });
        verify(repositorio, never()).save(any());
    }
}
