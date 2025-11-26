package com.kitchensink.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UpdateRequestResponseDTO Tests")
class UpdateRequestResponseDTOTest {

    private UpdateRequestResponseDTO dto;

    @BeforeEach
    void setUp() {
        dto = new UpdateRequestResponseDTO();
    }

    @Test
    @DisplayName("Should set and get all fields")
    void testGettersAndSetters() {
        LocalDateTime now = LocalDateTime.now();
        dto.setId("req-1");
        dto.setUserId("user-1");
        dto.setRequestType("PROFILE_UPDATE");
        dto.setFieldName("name");
        dto.setStatus("PENDING");
        dto.setOldValue("Old Name");
        dto.setNewValue("New Name");
        dto.setRequestedAt(now);
        dto.setReviewedAt(now);
        dto.setReviewedBy("admin-1");
        dto.setRejectionReason("Invalid");

        assertThat(dto.getId()).isEqualTo("req-1");
        assertThat(dto.getUserId()).isEqualTo("user-1");
        assertThat(dto.getRequestType()).isEqualTo("PROFILE_UPDATE");
        assertThat(dto.getFieldName()).isEqualTo("name");
        assertThat(dto.getStatus()).isEqualTo("PENDING");
        assertThat(dto.getOldValue()).isEqualTo("Old Name");
        assertThat(dto.getNewValue()).isEqualTo("New Name");
        assertThat(dto.getRequestedAt()).isEqualTo(now);
        assertThat(dto.getReviewedAt()).isEqualTo(now);
        assertThat(dto.getReviewedBy()).isEqualTo("admin-1");
        assertThat(dto.getRejectionReason()).isEqualTo("Invalid");
    }
}

