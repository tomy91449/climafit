package ar.edu.utn.frc.climafit.notificaciones.servicio;

import ar.edu.utn.frc.climafit.commons.error.RecursoNoEncontradoException;
import ar.edu.utn.frc.climafit.commons.seguridad.UsuarioActual;
import ar.edu.utn.frc.climafit.notificaciones.dominio.Contacto;
import ar.edu.utn.frc.climafit.notificaciones.dto.ContactoRequest;
import ar.edu.utn.frc.climafit.notificaciones.dto.ContactoResponse;
import ar.edu.utn.frc.climafit.notificaciones.repositorio.ContactoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * F10: cada usuario (alumno o profesor) gestiona SOLO su propio contacto. Por eso la ruta es
 * /contactos/mio y el id sale del token: no hay forma de pedir o modificar el de otro.
 */
@Service
public class ContactoService {

    private final ContactoRepository repositorio;
    private final UsuarioActual usuarioActual;

    public ContactoService(ContactoRepository repositorio, UsuarioActual usuarioActual) {
        this.repositorio = repositorio;
        this.usuarioActual = usuarioActual;
    }

    /** PUT idempotente: la primera vez crea el contacto, las siguientes lo actualizan. */
    @Transactional
    public ContactoResponse guardarMio(ContactoRequest request) {
        String usuarioId = usuarioActual.id();
        Contacto contacto = repositorio.findById(usuarioId).orElseGet(() -> new Contacto(usuarioId));
        String telefono = request.telefono() == null || request.telefono().isBlank() ? null : request.telefono();
        contacto.actualizar(telefono, request.aceptaWhatsapp());
        return ContactoResponse.de(repositorio.save(contacto));
    }

    @Transactional(readOnly = true)
    public ContactoResponse obtenerMio() {
        return repositorio.findById(usuarioActual.id())
                .map(ContactoResponse::de)
                .orElseThrow(() -> new RecursoNoEncontradoException("Todavía no registraste tus preferencias de contacto"));
    }
}
