package com.taskflow.alwaysinprogressbackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Validation Errors (400)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {

        Map<String, String> fields = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                fields.put(error.getField(), error.getDefaultMessage())
        );

        return ResponseEntity.badRequest().body(Map.of(
                "error", "validation failed",
                "fields", fields
        ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {

        return ResponseEntity.badRequest().body(Map.of(
                "error", "invalid parameter",
                "message", ex.getName() + " must be a valid " + ex.getRequiredType().getSimpleName()
        ));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<?> handleOptimisticLockingFailure(
            ObjectOptimisticLockingFailureException ex) {

        return ResponseEntity.status(409).body(Map.of(
                "error", "conflict",
                "message", "Resource was updated by another user. Please refresh and try again."
        ));
    }

    // Not Found (404)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntime(RuntimeException ex) {

        String message = ex.getMessage();

        if ("NOT_FOUND".equals(message)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "not found"));
        }

        if ("FORBIDDEN".equals(message)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "forbidden"));
        }

        if ("INVALID_CREDENTIALS".equals(message)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid credentials"));
        }

        if ("INVALID_ASSIGNEE".equals(message)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "invalid assignee"));
        }

        if ("USER_ALREADY_EXISTS".equals(message)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "user already exists"));
        }

        // if("CONFLICT".equals(message)) {
        //     return ResponseEntity.status(409).body(Map.of(
        //             "error", "conflict",
        //             "message", "Resource was updated by another user. Please refresh and try again."
        //     ));
        // }

        // fallback
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "internal server error",
                        "timestamp", LocalDateTime.now()
                ));
    }
}