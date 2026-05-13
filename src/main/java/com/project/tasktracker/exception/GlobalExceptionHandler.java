package com.project.tasktracker.exception;

import com.project.tasktracker.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice //Handle all exceptions globally
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    //Handle ResourceNotFoundException
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {

        log.error("Resource not found: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                false,
                ex.getMessage(),
                null,
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException ex) {

        log.error("User already exists: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                false,
                ex.getMessage(),
                null,
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {

        log.error("Authentication failed: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                false,
                "Invalid email or password",
                null,
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    //Handle IllegalArgumentException
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {

        log.error("Invalid argument received: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                false,
                "Invalid value provided",
                null,
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {

        Map<String, String> validationErrors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                validationErrors.put(error.getField(), error.getDefaultMessage())
        );

        log.error("Validation failed: {}", validationErrors);

        ErrorResponse errorResponse = new ErrorResponse(
                false,
                "Validation failed",
                validationErrors,
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {

        log.error("Request body parse failed: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                false,
                "Invalid request body",
                null,
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    //Handle all other exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {

        log.error("Unexpected error occurred", ex);

        ErrorResponse errorResponse = new ErrorResponse(
                false,
                "Something went wrong",
                null,
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
