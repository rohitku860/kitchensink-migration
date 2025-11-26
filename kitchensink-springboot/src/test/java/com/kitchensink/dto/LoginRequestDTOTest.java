package com.kitchensink.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LoginRequestDTO Tests")
class LoginRequestDTOTest {

    private LoginRequestDTO dto;

    @BeforeEach
    void setUp() {
        dto = new LoginRequestDTO();
    }

    @Test
    @DisplayName("Should set and get all fields")
    void testGettersAndSetters() {
        dto.setEmail("test@example.com");
        dto.setOtp("123456");

        assertThat(dto.getEmail()).isEqualTo("test@example.com");
        assertThat(dto.getOtp()).isEqualTo("123456");
    }
}

