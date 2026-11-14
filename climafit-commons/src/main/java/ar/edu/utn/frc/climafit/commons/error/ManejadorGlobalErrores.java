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
 */
@RestControllerAdvice
public class ManejadorGlobalErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalErrores.class);

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ErrorResponse> negocio(NegocioException ex, HttpServletRequest request) {
        return responder(ex.getStatus(), ex.getCodigo(), ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> campos.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors()
                .forEach(error -> campos.putIfAbsent(error.getObjectName(), error.getDefaultMessage()));
        return responder(HttpStatus.BAD_REQUEST, CodigosError.DATOS_INVALIDOS,
                "Los datos enviados no son válidos", request, campos);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> validacionParametros(HandlerMethodValidationException ex,
                                                              HttpServletRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(resultado -> resultado.getResolvableErrors().forEach(error ->
                campos.putIfAbsent(resultado.getMethodParameter().getParameterName(), error.getDefaultMessage())));
        return responder(HttpStatus.BAD_REQUEST, CodigosError.DATOS_INVALIDOS,
                "Los datos enviados no son válidos", request, campos);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> restriccion(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violacion ->
                campos.putIfAbsent(violacion.getPropertyPath().toString(), violacion.getMessage()));
        return responder(HttpStatus.BAD_REQUEST, CodigosError.DATOS_INVALIDOS,
                "Los datos enviados no son válidos", request, campos);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> formato(Exception ex, HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST, CodigosError.DATOS_INVALIDOS,
                "El pedido tiene un formato inválido o le faltan datos", request);
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ErrorResponse> accesoDenegado(Exception ex, HttpServletRequest request) {
        return responder(HttpStatus.FORBIDDEN, CodigosError.ACCESO_DENEGADO,
                "No tenés permisos para realizar esta operación", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> rutaInexistente(NoResourceFoundException ex, HttpServletRequest request) {
        return responder(HttpStatus.NOT_FOUND, CodigosError.NO_ENCONTRADO, "El recurso no existe", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> metodoNoSoportado(HttpRequestMethodNotSupportedException ex,
                                                           HttpServletRequest request) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, CodigosError.DATOS_INVALIDOS,
                "Método HTTP no soportado para esta ruta", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(Exception ex, HttpServletRequest request) {
        log.error("Error inesperado en {}", request.getRequestURI(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, CodigosError.ERROR_INTERNO,
                "Ocurrió un error inesperado", request);
    }

    private ResponseEntity<ErrorResponse> responder(HttpStatus status, String codigo, String mensaje,
                                                    HttpServletRequest request) {
        return responder(status, codigo, mensaje, request, null);
    }

    private ResponseEntity<ErrorResponse> responder(HttpStatus status, String codigo, String mensaje,
                                                    HttpServletRequest request, Map<String, String> campos) {
        return ResponseEntity.status(status)
                .body(ErrorResponseFactory.crear(status.value(), codigo, mensaje, request,
                        campos == null || campos.isEmpty() ? null : campos));
    }
}
