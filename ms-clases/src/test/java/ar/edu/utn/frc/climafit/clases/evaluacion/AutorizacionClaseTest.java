package ar.edu.utn.frc.climafit.clases.evaluacion;

import static ar.edu.utn.frc.climafit.clases.evaluacion.Datos.clase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.utn.frc.climafit.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.climafit.commons.seguridad.UsuarioActual;
import org.springframework.http.HttpStatus;
import org.junit.jupiter.api.Test;

/** RN05: solo el profesor de la clase o un administrador. */
class AutorizacionClaseTest {

    private static AutorizacionClase como(String id, boolean admin) {
        UsuarioActual usuario = org.mockito.Mockito.mock(UsuarioActual.class);
        org.mockito.Mockito.when(usuario.id()).thenReturn(id);
        org.mockito.Mockito.when(usuario.esAdmin()).thenReturn(admin);
        return new AutorizacionClase(usuario);
    }

    @Test
    void rn05_elProfesorDuenoPuedeEvaluar() {
        como("profe1", false).verificarProfesorDuenoOAdmin(clase(1, 6));
    }

    @Test
    void rn05_unAdministradorPuedeEvaluarCualquierClase() {
        como("admin1", true).verificarProfesorDuenoOAdmin(clase(1, 6));
    }

    @Test
    void rn05_otroProfesorRecibe403() {
        AccesoDenegadoException e = assertThrows(AccesoDenegadoException.class,
                () -> como("profe2", false).verificarProfesorDuenoOAdmin(clase(1, 6)));
        assertEquals("ACCESO_DENEGADO", e.getCodigo());
        assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
    }

    @Test
    void rn05_laDecisionSobreUnaClaseEnRiesgoEsSoloDelProfesorDueno_tampocoElAdmin() {
        como("profe1", false).verificarProfesorDueno(clase(1, 6));
        assertThrows(AccesoDenegadoException.class, () -> como("profe2", false).verificarProfesorDueno(clase(1, 6)));
        assertThrows(AccesoDenegadoException.class, () -> como("admin1", true).verificarProfesorDueno(clase(1, 6)));
    }
}
