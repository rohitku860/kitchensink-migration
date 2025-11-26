package com.kitchensink.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FieldUpdateRequestDTO Tests")
class FieldUpdateRequestDTOTest {

    private FieldUpdateRequestDTO dto;

    @BeforeEach
    void setUp() {
        dto = new FieldUpdateRequestDTO();
    }

    @Test
    @DisplayName("Should set and get all fields")
    void testGettersAndSetters() {
        dto.setFieldName("name");
        dto.setValue("New Name");
        dto.setOtp("123456");
        dto.setIsdCode("+91");

        assertThat(dto.getFieldName()).isEqualTo("name");
        assertThat(dto.getValue()).isEqualTo("New Name");
        assertThat(dto.getOtp()).isEqualTo("123456");
        assertThat(dto.getIsdCode()).isEqualTo("+91");
    }

    @Test
    @DisplayName("Should create with constructor")
    void testConstructor() {
        FieldUpdateRequestDTO newDto = new FieldUpdateRequestDTO("name", "New Name");

        assertThat(newDto.getFieldName()).isEqualTo("name");
        assertThat(newDto.getValue()).isEqualTo("New Name");
    }
}

