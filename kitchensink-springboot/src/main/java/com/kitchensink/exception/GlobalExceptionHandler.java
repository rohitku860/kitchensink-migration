package com.kitchensink.exception;

import com.kitchensink.dto.Response;
import com.kitchensink.util.CorrelationIdUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    // Handle validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Response<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        
        // Collect all field errors
        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError) {
                FieldError fieldError = (FieldError) error;
                String fieldName = fieldError.getField();
                String errorMessage = fieldError.getDefaultMessage() != null 
                        ? fieldError.getDefaultMessage() 
                        : "Validation failed for " + fieldName;
                errors.put(fieldName, errorMessage);
            } else {
                // Handle non-field errors (global errors)
                String errorMessage = error.getDefaultMessage() != null 
                        ? error.getDefaultMessage() 
                        : "Validation failed";
                errors.put(error.getObjectName(), errorMessage);
            }
        });
        
        // Create a descriptive error message listing all failed fields
        String errorMessage = "Validation failed";
        if (!errors.isEmpty()) {
            String fieldsList = String.join(", ", errors.keySet());
            errorMessage = "Validation failed for field(s): " + fieldsList;
        }
        
        logger.warn("Validation failed: {}", errors);
        Response<Map<String, String>> response = Response.error(
                errorMessage, "VALIDATION_ERROR", errors.toString());
        response.setCorrelationId(CorrelationIdUtil.getCorrelationId());
        response.setData(errors);
        return ResponseEntity.badRequest().body(response);
    }
    
    // Handle resource not found exceptions
    @ExceptionHandler(com.kitchensink.exception.ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<Response<Void>> handleResourceNotFoundException(
            com.kitchensink.exception.ResourceNotFoundException ex) {
        logger.warn("Resource not found: {}", ex.getMessage());
        Response<Void> response = Response.error(ex.getMessage(), "RESOURCE_NOT_FOUND");
        response.setCorrelationId(CorrelationIdUtil.getCorrelationId());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    // Handle runtime exceptions (fallback)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Response<Void>> handleRuntimeException(RuntimeException ex) {
        logger.error("Runtime exception occurred", ex);
        Response<Void> response = Response.error(
                "An error occurred while processing your request", "INTERNAL_ERROR", ex.getMessage());
        response.setCorrelationId(CorrelationIdUtil.getCorrelationId());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    // Handle resource conflict exceptions (duplicate email/phone)
    @ExceptionHandler(com.kitchensink.exception.ResourceConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<Response<Map<String, String>>> handleResourceConflictException(
            com.kitchensink.exception.ResourceConflictException ex) {
        logger.warn("Resource conflict: {} - {}", ex.getField(), ex.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put(ex.getField(), ex.getMessage());
        Response<Map<String, String>> response = Response.error(
                ex.getMessage(), "RESOURCE_CONFLICT", ex.getField());
        response.setCorrelationId(CorrelationIdUtil.getCorrelationId());
        response.setData(error);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
    
    // Handle duplicate key exceptions (MongoDB unique constraint violations)
    @ExceptionHandler(org.springframework.dao.DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<Response<Map<String, String>>> handleDuplicateKeyException(
            org.springframework.dao.DuplicateKeyException ex) {
        logger.warn("Duplicate key violation: {}", ex.getMessage());
        Map<String, String> error = new HashMap<>();
        String message = ex.getMessage();
        String field = "error";
        String errorMessage = "Duplicate key violation";
        
        if (message != null) {
            if (message.contains("email")) {
                field = "email";
                errorMessage = "Email already exists";
            } else if (message.contains("phoneNumber")) {
                field = "phoneNumber";
                errorMessage = "Phone number already exists";
            }
        }
        error.put(field, errorMessage);
        
        Response<Map<String, String>> response = Response.error(
                errorMessage, "DUPLICATE_KEY", field);
        response.setCorrelationId(CorrelationIdUtil.getCorrelationId());
        response.setData(error);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
}

