package com.coffeeshop.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> notFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return response(HttpStatus.NOT_FOUND, ex.getMessage(), req, Map.of());
    }

    @ExceptionHandler(DuplicateSkuException.class)
    public ResponseEntity<ApiError> duplicateSku(DuplicateSkuException ex, HttpServletRequest req) {
        return response(HttpStatus.CONFLICT, ex.getMessage(), req, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e ->
                errors.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return response(HttpStatus.BAD_REQUEST, "Request validation failed", req, errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return response(HttpStatus.BAD_REQUEST, "Request body is missing or malformed", req, Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        return response(HttpStatus.CONFLICT, "The request conflicts with existing data", req, Map.of());
    }

    private ResponseEntity<ApiError> response(
            HttpStatus status, String message, HttpServletRequest req, Map<String, String> errors) {
        return ResponseEntity.status(status).body(new ApiError(
                Instant.now(), status.value(), status.getReasonPhrase(),
                message, req.getRequestURI(), errors));
    }
}
