package com.synapse.waypoint.common.error;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Maps exceptions to the standard {@link ApiError} body. Internal details never reach the client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String CONFLICT_MESSAGE = "This was changed by someone else. Please reload.";

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> handleDomain(DomainException ex) {
        return respond(ex.code(), ApiError.of(ex.code(), ex.getMessage(), ex.details()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleInvalidBody(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return respond(ErrorCode.VALIDATION, ApiError.of(ErrorCode.VALIDATION, "Some fields are not valid.", fields));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiError> handleBadRequest(Exception ex) {
        return respond(ErrorCode.VALIDATION, ApiError.of(ErrorCode.VALIDATION, "The request is not valid."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return respond(ErrorCode.FORBIDDEN, ApiError.of(ErrorCode.FORBIDDEN, "You don't have access to this."));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleConcurrentChange(OptimisticLockingFailureException ex) {
        return respond(ErrorCode.CONFLICT, ApiError.of(ErrorCode.CONFLICT, CONFLICT_MESSAGE));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoRoute(NoResourceFoundException ex) {
        return respond(ErrorCode.NOT_FOUND, ApiError.of(ErrorCode.NOT_FOUND, "Not found."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Unhandled error", ex);
        return respond(ErrorCode.INTERNAL, ApiError.of(ErrorCode.INTERNAL, "Something went wrong. Please try again."));
    }

    private static ResponseEntity<ApiError> respond(ErrorCode code, ApiError body) {
        return ResponseEntity.status(code.status()).body(body);
    }
}
