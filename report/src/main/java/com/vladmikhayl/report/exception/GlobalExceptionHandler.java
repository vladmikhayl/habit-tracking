package com.vladmikhayl.report.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                Map.of(
                        "error", ex.getMessage(),
                        "timestamp", LocalDateTime.now()
                )
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(
                Map.of(
                        "error", ex.getReason(),
                        "timestamp", LocalDateTime.now()
                )
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                Map.of(
                        "error", ex.getMessage(),
                        "timestamp", LocalDateTime.now()
                )
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                Map.of(
                        "error", ex.getMessage(),
                        "timestamp", LocalDateTime.now()
                )
        );
    }

    // Это исключение выкидывается только когда какие-то переданные в запрос параметры с @Valid не прошли валидацию
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<String> errors = new ArrayList<>();

        ex.getBindingResult().getFieldErrors().forEach(fieldError ->
                errors.add(fieldError.getDefaultMessage()));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                Map.of(
                        "errors", errors,
                        "timestamp", LocalDateTime.now()
                )
        );
    }

}
