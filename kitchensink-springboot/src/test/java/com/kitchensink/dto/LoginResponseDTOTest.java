package com.kitchensink.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LoginResponseDTO Tests")
class LoginResponseDTOTest {

    private LoginResponseDTO dto;

    @BeforeEach
    void setUp() {
        dto = new LoginResponseDTO();
    }

    @Test
    @DisplayName("Should set and get all fields")
    void testGettersAndSetters() {
        dto.setToken("jwt-token");
        dto.setUserId("user-1");
        dto.setRole("USER");
        dto.setRoleId("role-1");
        dto.setEmail("test@example.com");
        dto.setName("Test User");

        assertThat(dto.getToken()).isEqualTo("jwt-token");
        assertThat(dto.getUserId()).isEqualTo("user-1");
        assertThat(dto.getRole()).isEqualTo("USER");
        assertThat(dto.getRoleId()).isEqualTo("role-1");
        assertThat(dto.getEmail()).isEqualTo("test@example.com");
        assertThat(dto.getName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("Should create with constructor")
    void testConstructor() {
        LoginResponseDTO newDto = new LoginResponseDTO("token", "user-1", "USER", "role-1", "test@example.com", "Test User");

        assertThat(newDto.getToken()).isEqualTo("token");
        assertThat(newDto.getUserId()).isEqualTo("user-1");
        assertThat(newDto.getRole()).isEqualTo("USER");
        assertThat(newDto.getRoleId()).isEqualTo("role-1");
        assertThat(newDto.getEmail()).isEqualTo("test@example.com");
        assertThat(newDto.getName()).isEqualTo("Test User");
    }
}

