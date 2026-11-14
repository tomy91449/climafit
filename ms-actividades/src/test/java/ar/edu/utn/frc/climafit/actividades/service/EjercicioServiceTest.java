package ar.edu.utn.frc.climafit.actividades.service;

import ar.edu.utn.frc.climafit.actividades.dto.CompatibilidadRequest;
import ar.edu.utn.frc.climafit.actividades.dto.CompatibilidadResponse;
import ar.edu.utn.frc.climafit.actividades.dto.EjercicioRequest;
import ar.edu.utn.frc.climafit.actividades.dto.EjercicioResponse;
import ar.edu.utn.frc.climafit.actividades.entity.Ejercicio;
import ar.edu.utn.frc.climafit.actividades.entity.Nivel;
import ar.edu.utn.frc.climafit.actividades.entity.TipoActividad;
import ar.edu.utn.frc.climafit.actividades.repository.EjercicioRepository;
import ar.edu.utn.frc.climafit.actividades.repository.TipoActividadRepository;
import ar.edu.utn.frc.climafit.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.climafit.commons.error.NegocioException;
import ar.edu.utn.frc.climafit.commons.error.RecursoNoEncontradoException;
import ar.edu.utn.frc.climafit.commons.seguridad.UsuarioActual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Reglas RN07 y RN08 en la capa service, sin base de datos. */
class EjercicioServiceTest {

    private static final String PROFE_1 = "profe-1";
    private static final String PROFE_2 = "profe-2";

    private final EjercicioRepository ejercicios = mock(EjercicioRepository.class);
    private final TipoActividadRepository actividades = mock(TipoActividadRepository.class);
    private final UsuarioActual usuario = mock(UsuarioActual.class);
    private final EjercicioService servicio = new EjercicioService(ejercicios, actividades, usuario);

    private TipoActividad yoga;
    private TipoActividad funcional;

    @BeforeEach
    void datos() {
        yoga = conId(new TipoActividad("Yoga", null, 60, new BigDecimal("0.5"), 35, 5), 1L);
        funcional = conId(new TipoActividad("Funcional", null, 70, new BigDecimal("2.0"), 35, 5), 2L);
        when(actividades.existsById(1L)).thenReturn(true);
        when(ejercicios.save(any(Ejercicio.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    // ---------- RN07: compatibilidad de ejercicios ----------

    @Test
    void rn07_ejerciciosDeLaActividadSonCompatibles() {
        Ejercicio saludo = ejercicio(1L, null, yoga);
        Ejercicio arbol = ejercicio(2L, null, yoga);
        when(ejercicios.findAllById(List.of(1L, 2L))).thenReturn(List.of(saludo, arbol));

        CompatibilidadResponse respuesta = servicio.verificarCompatibilidad(new CompatibilidadRequest(1L, List.of(1L, 2L)));

        assertThat(respuesta.compatible()).isTrue();
        assertThat(respuesta.incompatibles()).isEmpty();
    }

    @Test
    void rn07_unEjercicioDeOtraActividadEsIncompatible() {
        Ejercicio saludo = ejercicio(1L, null, yoga);
        Ejercicio burpees = ejercicio(3L, null, funcional);
        when(ejercicios.findAllById(List.of(1L, 3L))).thenReturn(List.of(saludo, burpees));

        CompatibilidadResponse respuesta = servicio.verificarCompatibilidad(new CompatibilidadRequest(1L, List.of(1L, 3L)));

        assertThat(respuesta.compatible()).isFalse();
        assertThat(respuesta.incompatibles()).containsExactly(3L);
    }

    @Test
    void rn07_unEjercicioDadoDeBajaEsIncompatible() {
        Ejercicio saludo = ejercicio(1L, null, yoga);
        saludo.darDeBaja();
        when(ejercicios.findAllById(List.of(1L))).thenReturn(List.of(saludo));

        CompatibilidadResponse respuesta = servicio.verificarCompatibilidad(new CompatibilidadRequest(1L, List.of(1L)));

        assertThat(respuesta.incompatibles()).containsExactly(1L);
    }

    @Test
    void rn07_unEjercicioInexistenteEsIncompatible() {
        when(ejercicios.findAllById(List.of(99L))).thenReturn(List.of());

        CompatibilidadResponse respuesta = servicio.verificarCompatibilidad(new CompatibilidadRequest(1L, List.of(99L)));

        assertThat(respuesta.incompatibles()).containsExactly(99L);
    }

    @Test
    void rn07_unaActividadInexistenteResponde404() {
        assertThatThrownBy(() -> servicio.verificarCompatibilidad(new CompatibilidadRequest(999L, List.of(1L))))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // ---------- RN08: propiedad de los ejercicios ----------

    @Test
    void rn08_unProfesorCreaUnEjercicioPersonalizadoASuNombre() {
        comoProfesor(PROFE_1);
        when(actividades.findAllById(anyList())).thenReturn(List.of(yoga));

        EjercicioResponse creado = servicio.crear(pedido(List.of(1L)));

        assertThat(creado.propietarioId()).isEqualTo(PROFE_1);
    }

    @Test
    void rn08_elAdministradorCreaUnEjercicioBase() {
        comoAdmin();
        when(actividades.findAllById(anyList())).thenReturn(List.of(yoga));

        EjercicioResponse creado = servicio.crear(pedido(List.of(1L)));

        assertThat(creado.propietarioId()).isNull();
    }

    @Test
    void rn08_unaActividadCompatibleInexistenteResponde400() {
        comoProfesor(PROFE_1);
        when(actividades.findAllById(anyList())).thenReturn(List.of(yoga));

        assertThatThrownBy(() -> servicio.crear(pedido(List.of(1L, 999L))))
                .isInstanceOf(NegocioException.class)
                .extracting("codigo").isEqualTo("DATOS_INVALIDOS");
    }

    @Test
    void rn08_elDuenioVeSuEjercicioPersonalizado() {
        comoProfesor(PROFE_1);
        when(ejercicios.findById(10L)).thenReturn(Optional.of(ejercicio(10L, PROFE_1, yoga)));

        assertThat(servicio.obtener(10L).propietarioId()).isEqualTo(PROFE_1);
    }

    @Test
    void rn08_otroProfesorNoVeElEjercicioPersonalizadoYRecibe404() {
        comoProfesor(PROFE_2);
        when(ejercicios.findById(10L)).thenReturn(Optional.of(ejercicio(10L, PROFE_1, yoga)));

        assertThatThrownBy(() -> servicio.obtener(10L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void rn08_cualquieraVeUnEjercicioBase() {
        comoProfesor(PROFE_2);
        when(ejercicios.findById(1L)).thenReturn(Optional.of(ejercicio(1L, null, yoga)));

        assertThat(servicio.obtener(1L).propietarioId()).isNull();
    }

    @Test
    void rn08_unProfesorNoPuedeModificarUnEjercicioBaseYRecibe403() {
        comoProfesor(PROFE_1);
        when(ejercicios.findById(1L)).thenReturn(Optional.of(ejercicio(1L, null, yoga)));

        assertThatThrownBy(() -> servicio.actualizar(1L, pedido(List.of(1L))))
                .isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> servicio.darDeBaja(1L)).isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void rn08_elAdministradorModificaUnEjercicioBase() {
        comoAdmin();
        when(ejercicios.findById(1L)).thenReturn(Optional.of(ejercicio(1L, null, yoga)));
        when(actividades.findAllById(anyList())).thenReturn(List.of(yoga));

        assertThat(servicio.actualizar(1L, pedido(List.of(1L))).nombre()).isEqualTo("Respiración en parque");
    }

    @Test
    void rn08_otroProfesorNoPuedeModificarNiDarDeBajaUnPersonalizadoYRecibe404() {
        comoProfesor(PROFE_2);
        when(ejercicios.findById(10L)).thenReturn(Optional.of(ejercicio(10L, PROFE_1, yoga)));

        assertThatThrownBy(() -> servicio.actualizar(10L, pedido(List.of(1L))))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.darDeBaja(10L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void rn08_elAdministradorNoVeNiModificaUnPersonalizado() {
        comoAdmin();
        when(ejercicios.findById(10L)).thenReturn(Optional.of(ejercicio(10L, PROFE_1, yoga)));

        assertThatThrownBy(() -> servicio.obtener(10L)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.darDeBaja(10L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void rn08_laBajaEsLogica() {
        comoProfesor(PROFE_1);
        Ejercicio propio = ejercicio(10L, PROFE_1, yoga);
        when(ejercicios.findById(10L)).thenReturn(Optional.of(propio));

        servicio.darDeBaja(10L);

        assertThat(propio.estaActivo()).isFalse();
    }

    // ---------- utilidades ----------

    private void comoProfesor(String sub) {
        when(usuario.id()).thenReturn(sub);
        when(usuario.esAdmin()).thenReturn(false);
    }

    private void comoAdmin() {
        when(usuario.id()).thenReturn("admin-1");
        when(usuario.esAdmin()).thenReturn(true);
    }

    private static EjercicioRequest pedido(List<Long> actividades) {
        return new EjercicioRequest("Respiración en parque", "Respiración diafragmática", Nivel.INICIAL, actividades);
    }

    private static Ejercicio ejercicio(Long id, String propietario, TipoActividad... compatibles) {
        return conId(new Ejercicio("Ejercicio " + id, null, Nivel.INICIAL, propietario, List.of(compatibles)), id);
    }

    private static <T> T conId(T entidad, Long id) {
        ReflectionTestUtils.setField(entidad, "id", id);
        return entidad;
    }
}
