package com.kitchensink.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserResponseDTO Tests")
class UserResponseDTOTest {

    private UserResponseDTO dto;

    @BeforeEach
    void setUp() {
        dto = new UserResponseDTO();
    }

    @Test
    @DisplayName("Should set and get all fields")
    void testGettersAndSetters() {
        LocalDateTime now = LocalDateTime.now();
        dto.setId("user-1");
        dto.setName("Test User");
        dto.setEmail("test@example.com");
        dto.setIsdCode("+91");
        dto.setPhoneNumber("9876543210");
        dto.setDateOfBirth("01-01-1990");
        dto.setAddress("Address");
        dto.setCity("City");
        dto.setCountry("Country");
        dto.setRole("USER");
        dto.setRegistrationDate(now);
        dto.setLastLoginDate(now);
        dto.setStatus("ACTIVE");

        assertThat(dto.getId()).isEqualTo("user-1");
        assertThat(dto.getName()).isEqualTo("Test User");
        assertThat(dto.getEmail()).isEqualTo("test@example.com");
        assertThat(dto.getIsdCode()).isEqualTo("+91");
        assertThat(dto.getPhoneNumber()).isEqualTo("9876543210");
        assertThat(dto.getDateOfBirth()).isEqualTo("01-01-1990");
        assertThat(dto.getAddress()).isEqualTo("Address");
        assertThat(dto.getCity()).isEqualTo("City");
        assertThat(dto.getCountry()).isEqualTo("Country");
        assertThat(dto.getRole()).isEqualTo("USER");
        assertThat(dto.getRegistrationDate()).isEqualTo(now);
        assertThat(dto.getLastLoginDate()).isEqualTo(now);
        assertThat(dto.getStatus()).isEqualTo("ACTIVE");
    }
}

