package com.kitchensink.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResourceNotFoundException Tests")
class ResourceNotFoundExceptionTest {

    @Test
    @DisplayName("Should create exception with resource name and id")
    void testConstructor_WithResourceAndId() {
        ResourceNotFoundException exception = new ResourceNotFoundException("User", "user-1");

        assertThat(exception.getMessage()).contains("User");
        assertThat(exception.getMessage()).contains("user-1");
    }

    @Test
    @DisplayName("Should create exception with custom message")
    void testConstructor_WithMessage() {
        String message = "Custom error message";
        ResourceNotFoundException exception = new ResourceNotFoundException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
    }
}

