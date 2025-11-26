package com.kitchensink.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResourceConflictException Tests")
class ResourceConflictExceptionTest {

    @Test
    @DisplayName("Should create exception with message and field")
    void testConstructor_WithMessageAndField() {
        ResourceConflictException exception = new ResourceConflictException("Email already exists", "email");

        assertThat(exception.getMessage()).isEqualTo("Email already exists");
        assertThat(exception.getField()).isEqualTo("email");
    }

    @Test
    @DisplayName("Should create exception with null field")
    void testConstructor_WithNullField() {
        ResourceConflictException exception = new ResourceConflictException("Resource conflict", null);

        assertThat(exception.getMessage()).isEqualTo("Resource conflict");
        assertThat(exception.getField()).isNull();
    }
}

