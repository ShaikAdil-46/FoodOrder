package com.foodorder.common;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalidBody(MethodArgumentNotValidException exception,
                                                HttpServletRequest request) {
        var fields = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(error -> error.getField(), error -> error.getDefaultMessage(),
                        (first, ignored) -> first));
        return response(HttpStatus.BAD_REQUEST, "Please check the highlighted fields.", request, fields);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> invalidParameter(HandlerMethodValidationException exception,
                                                    HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "One or more request parameters are invalid.", request, Map.of());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> notFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> invalidRequest(IllegalArgumentException exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> badCredentials(BadCredentialsException exception, HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiError> forbidden(AuthorizationDeniedException exception, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, "You do not have permission to perform this action.", request, Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> conflict(DataIntegrityViolationException exception,
                                             HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "This record conflicts with existing data.", request, Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception exception, HttpServletRequest request) {
        logger.error("Unhandled API exception for {} {}", request.getMethod(), request.getRequestURI(), exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again.", request, Map.of());
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String message, HttpServletRequest request,
                                              Map<String, String> fields) {
        return ResponseEntity.status(status.value()).body(new ApiError(Instant.now(), status.value(), message,
                request.getRequestURI(), fields));
    }

    public record ApiError(Instant timestamp, int status, String message, String path,
                           Map<String, String> fieldErrors) {}
}
