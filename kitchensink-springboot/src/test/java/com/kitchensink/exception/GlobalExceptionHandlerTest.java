package com.kitchensink.exception;

import com.kitchensink.dto.Response;
import com.kitchensink.util.CorrelationIdUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        // Set correlation ID for tests using MDC
        MDC.put("correlationId", "test-correlation-id");
    }

    @Test
    @DisplayName("Should handle validation exceptions")
    void testHandleValidationExceptions() {
        // Given
        BindingResult bindingResult = mock(BindingResult.class);
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        
        List<FieldError> fieldErrors = new ArrayList<>();
        fieldErrors.add(new FieldError("member", "email", "Email is required"));
        fieldErrors.add(new FieldError("member", "name", "Name must not be blank"));

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(new ArrayList<>(fieldErrors));

        // When
        ResponseEntity<Response<Map<String, String>>> response = exceptionHandler.handleValidationExceptions(ex);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getError().getCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getCorrelationId()).isEqualTo("test-correlation-id");
    }

    @Test
    @DisplayName("Should handle ResourceNotFoundException")
    void testHandleResourceNotFoundException() {
        // Given
        ResourceNotFoundException ex = new ResourceNotFoundException("Member", "test-id");

        // When
        ResponseEntity<Response<Void>> response = exceptionHandler.handleResourceNotFoundException(ex);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getError().getCode()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(response.getBody().getMessage()).contains("Member");
        assertThat(response.getBody().getCorrelationId()).isEqualTo("test-correlation-id");
    }

    @Test
    @DisplayName("Should handle ResourceConflictException")
    void testHandleResourceConflictException() {
        // Given
        ResourceConflictException ex = new ResourceConflictException("Email already exists", "email");

        // When
        ResponseEntity<Response<Map<String, String>>> response = exceptionHandler.handleResourceConflictException(ex);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getError().getCode()).isEqualTo("RESOURCE_CONFLICT");
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().get("email")).isEqualTo("Email already exists");
        assertThat(response.getBody().getCorrelationId()).isEqualTo("test-correlation-id");
    }

    @Test
    @DisplayName("Should handle DuplicateKeyException for email")
    void testHandleDuplicateKeyException_Email() {
        // Given
        DuplicateKeyException ex = new DuplicateKeyException("Duplicate key: emailHash");

        // When
        ResponseEntity<Response<Map<String, String>>> response = exceptionHandler.handleDuplicateKeyException(ex);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getError().getCode()).isEqualTo("DUPLICATE_KEY");
        assertThat(response.getBody().getData().get("email")).isEqualTo("Email already exists");
    }

    @Test
    @DisplayName("Should handle DuplicateKeyException for phone number")
    void testHandleDuplicateKeyException_PhoneNumber() {
        // Given
        DuplicateKeyException ex = new DuplicateKeyException("Duplicate key: phoneNumberHash");

        // When
        ResponseEntity<Response<Map<String, String>>> response = exceptionHandler.handleDuplicateKeyException(ex);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().get("phoneNumber")).isEqualTo("Phone number already exists");
    }

    @Test
    @DisplayName("Should handle RuntimeException")
    void testHandleRuntimeException() {
        // Given
        RuntimeException ex = new RuntimeException("Unexpected error occurred");

        // When
        ResponseEntity<Response<Void>> response = exceptionHandler.handleRuntimeException(ex);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getError().getCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().getCorrelationId()).isEqualTo("test-correlation-id");
    }
}

