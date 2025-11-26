package com.kitchensink.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserRequestDTO Tests")
class UserRequestDTOTest {

    private UserRequestDTO dto;

    @BeforeEach
    void setUp() {
        dto = new UserRequestDTO();
    }

    @Test
    @DisplayName("Should set and get all fields")
    void testGettersAndSetters() {
        dto.setName("Test User");
        dto.setEmail("test@example.com");
        dto.setIsdCode("+91");
        dto.setPhoneNumber("9876543210");
        dto.setDateOfBirth("01-01-1990");
        dto.setAddress("Address");
        dto.setCity("City");
        dto.setCountry("Country");
        dto.setRole("USER");

        assertThat(dto.getName()).isEqualTo("Test User");
        assertThat(dto.getEmail()).isEqualTo("test@example.com");
        assertThat(dto.getIsdCode()).isEqualTo("+91");
        assertThat(dto.getPhoneNumber()).isEqualTo("9876543210");
        assertThat(dto.getDateOfBirth()).isEqualTo("01-01-1990");
        assertThat(dto.getAddress()).isEqualTo("Address");
        assertThat(dto.getCity()).isEqualTo("City");
        assertThat(dto.getCountry()).isEqualTo("Country");
        assertThat(dto.getRole()).isEqualTo("USER");
    }

    @Test
    @DisplayName("Should have default role as USER")
    void testDefaultRole() {
        assertThat(dto.getRole()).isEqualTo("USER");
    }
}

