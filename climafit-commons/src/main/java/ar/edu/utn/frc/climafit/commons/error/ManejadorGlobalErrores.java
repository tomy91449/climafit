package ar.edu.utn.frc.climafit.commons.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce las excepciones de todos los servicios al formato común {@link ErrorResponse}.
 *
 * <p>Para principiantes: cuando un controller (o algo que él llama) lanza una excepción y nadie la
 * atrapa, Spring busca un método marcado con {@code @ExceptionHandler} para ese tipo de excepción y
 * lo ejecuta. Lo que ese método devuelve es la respuesta HTTP. Si hay varios que sirven, gana el más
 * específico; {@code Exception.class} (el último) atrapa todo lo que no atrapó ningún otro.
 *
 * <p>{@code @RestControllerAdvice} = "este manejo de errores aplica a todos los controllers del
 * servicio". Lo activa ClimafitErroresAutoConfiguration en cada microservicio. Es el equivalente,
 * en los microservicios, de ManejadorErroresGateway del API Gateway.
 */
@RestControllerAdvice
public class ManejadorGlobalErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalErrores.class);

    /**
     * Errores de negocio (y sus "atajos": 404, 403, 503). Cada excepción ya trae su status, código y
     * mensaje, así que solo se copian a la respuesta. Ejemplo: 409 CUPO_COMPLETO.
     */
    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ErrorResponse> negocio(NegocioException ex, HttpServletRequest request) {
        return responder(ex.getStatus(), ex.getCodigo(), ex.getMessage(), request);
    }

    /**
     * 400: falló la validación del cuerpo JSON ({@code @Valid @RequestBody}), por ejemplo un
     * {@code @NotNull} sin valor. Se arma el detalle "campo -> mensaje" para el campo "campos".
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
        // LinkedHashMap mantiene el orden en que se agregan los errores.
        Map<String, String> campos = new LinkedHashMap<>();
        // Errores de un campo puntual (por ejemplo "inicio": "no debe ser nulo").
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> campos.putIfAbsent(error.getField(), error.getDefaultMessage()));
        // Errores de todo el objeto (por ejemplo "el fin tiene que ser posterior al inicio").
        ex.getBindingResult().getGlobalErrors()
                .forEach(error -> campos.putIfAbsent(error.getObjectName(), error.getDefaultMessage()));
        return responder(HttpStatus.BAD_REQUEST, CodigosError.DATOS_INVALIDOS,
                "Los datos enviados no son válidos", request, campos);
    }

    /** 400: falló la validación de un parámetro del método (de la URL o de la query, por ejemplo ?cupo=-1). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> validacionParametros(HandlerMethodValidationException ex,
                                                              HttpServletRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(resultado -> resultado.getResolvableErrors().forEach(error ->
                campos.putIfAbsent(resultado.getMethodParameter().getParameterName(), error.getDefaultMessage())));
        return responder(HttpStatus.BAD_REQUEST, CodigosError.DATOS_INVALIDOS,
                "Los datos enviados no son válidos", request, campos);
    }

    /** 400: falló una validación hecha fuera del controller (por ejemplo en un servicio con {@code @Validated}). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> restriccion(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violacion ->
                campos.putIfAbsent(violacion.getPropertyPath().toString(), violacion.getMessage()));
        return responder(HttpStatus.BAD_REQUEST, CodigosError.DATOS_INVALIDOS,
                "Los datos enviados no son válidos", request, campos);
    }

    /**
     * 400: el pedido no se pudo ni leer. Por ejemplo: JSON mal escrito, una fecha con formato
     * incorrecto, texto donde va un número ("/clases/abc") o un parámetro obligatorio que falta.
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> formato(Exception ex, HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST, CodigosError.DATOS_INVALIDOS,
                "El pedido tiene un formato inválido o le faltan datos", request);
    }

    /** 403: lo rechazó un {@code @PreAuthorize} (por ejemplo, un alumno en un endpoint de PROFESOR). */
    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ErrorResponse> accesoDenegado(Exception ex, HttpServletRequest request) {
        return responder(HttpStatus.FORBIDDEN, CodigosError.ACCESO_DENEGADO,
                "No tenés permisos para realizar esta operación", request);
    }

    /** 404: la URL no corresponde a ningún endpoint del servicio. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> rutaInexistente(NoResourceFoundException ex, HttpServletRequest request) {
        return responder(HttpStatus.NOT_FOUND, CodigosError.NO_ENCONTRADO, "El recurso no existe", request);
    }

    /** 405: la URL existe pero no con ese método (por ejemplo un DELETE donde solo hay GET). */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> metodoNoSoportado(HttpRequestMethodNotSupportedException ex,
                                                           HttpServletRequest request) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, CodigosError.DATOS_INVALIDOS,
                "Método HTTP no soportado para esta ruta", request);
    }

    /**
     * 500: cualquier otro error (un bug). El detalle completo se escribe en el log para poder
     * investigarlo; al usuario solo se le muestra un mensaje genérico, para no revelar datos internos.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(Exception ex, HttpServletRequest request) {
        log.error("Error inesperado en {}", request.getRequestURI(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, CodigosError.ERROR_INTERNO,
                "Ocurrió un error inesperado", request);
    }

    // ---- Métodos de ayuda: arman la respuesta HTTP con el JSON común ----

    private ResponseEntity<ErrorResponse> responder(HttpStatus status, String codigo, String mensaje,
                                                    HttpServletRequest request) {
        return responder(status, codigo, mensaje, request, null);
    }

    private ResponseEntity<ErrorResponse> responder(HttpStatus status, String codigo, String mensaje,
                                                    HttpServletRequest request, Map<String, String> campos) {
        // ResponseEntity = respuesta HTTP completa: status + cuerpo. Si no hay campos, se manda null
        // para que "campos" no aparezca en el JSON.
        return ResponseEntity.status(status)
                .body(ErrorResponseFactory.crear(status.value(), codigo, mensaje, request,
                        campos == null || campos.isEmpty() ? null : campos));
    }
}
