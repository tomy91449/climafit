package ar.edu.utn.frc.climafit.commons.error;

import org.springframework.http.HttpStatus;

/**
 * Excepción base de los errores de negocio. Cada servicio la usa (o la extiende)
 * con su propio código, por ejemplo:
 * {@code throw new NegocioException(HttpStatus.CONFLICT, "CUPO_COMPLETO", "La clase no tiene lugares disponibles");}
 *
 * <p>Para principiantes: cuando una regla de negocio no se cumple, el código no devuelve un error a
 * mano: "lanza" (throw) esta excepción y corta ahí. Después {@link ManejadorGlobalErrores} la atrapa
 * y la convierte en la respuesta HTTP con el JSON de error. Así la lógica de negocio queda limpia.
 *
 * <p>Extiende {@link RuntimeException}: no obliga a poner try/catch ni "throws" en cada método.
 * Si la excepción ocurre dentro de un método {@code @Transactional}, los cambios en la base se deshacen.
 */
public class NegocioException extends RuntimeException {

    // Qué código HTTP devolver (409, 404, 403...).
    private final HttpStatus status;
    // Qué código de ClimaFit poner en el JSON ("CUPO_COMPLETO", "NO_ENCONTRADO"...).
    private final String codigo;

    public NegocioException(HttpStatus status, String codigo, String mensaje) {
        super(mensaje); // el mensaje queda guardado en la excepción (getMessage())
        this.status = status;
        this.codigo = codigo;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }
}
