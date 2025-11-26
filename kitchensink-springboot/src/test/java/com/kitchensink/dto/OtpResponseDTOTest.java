package com.kitchensink.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OtpResponseDTO Tests")
class OtpResponseDTOTest {

    private OtpResponseDTO dto;

    @BeforeEach
    void setUp() {
        dto = new OtpResponseDTO();
    }

    @Test
    @DisplayName("Should set and get message")
    void testGettersAndSetters() {
        dto.setMessage("OTP sent");

        assertThat(dto.getMessage()).isEqualTo("OTP sent");
    }

    @Test
    @DisplayName("Should create with constructor")
    void testConstructor() {
        OtpResponseDTO newDto = new OtpResponseDTO("OTP sent");

        assertThat(newDto.getMessage()).isEqualTo("OTP sent");
    }
}

