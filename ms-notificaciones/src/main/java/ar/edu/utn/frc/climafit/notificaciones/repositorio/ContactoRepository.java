package ar.edu.utn.frc.climafit.notificaciones.repositorio;

import ar.edu.utn.frc.climafit.notificaciones.dominio.Contacto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** La clave es el "sub" de Keycloak: findById(usuarioId) devuelve el contacto de ese usuario. */
public interface ContactoRepository extends JpaRepository<Contacto, String> {

    /** Contacto que generó el link de vinculación con ese código (el código es único). */
    Optional<Contacto> findByCodigoVinculacion(String codigoVinculacion);
}
