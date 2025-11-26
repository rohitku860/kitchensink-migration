package com.kitchensink.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UpdateRequest Model Tests")
class UpdateRequestTest {

    private UpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        updateRequest = new UpdateRequest();
    }

    @Test
    @DisplayName("Should create update request with default values")
    void testUpdateRequestCreation() {
        assertThat(updateRequest.getStatus()).isEqualTo("PENDING");
        assertThat(updateRequest.getRequestedAt()).isNotNull();
        assertThat(updateRequest.isOtpVerified()).isFalse();
    }

    @Test
    @DisplayName("Should create update request with constructor")
    void testUpdateRequestConstructor() {
        UpdateRequest request = new UpdateRequest("user-1", "PROFILE_UPDATE", "name", "Old Name", "New Name");

        assertThat(request.getUserId()).isEqualTo("user-1");
        assertThat(request.getRequestType()).isEqualTo("PROFILE_UPDATE");
        assertThat(request.getFieldName()).isEqualTo("name");
        assertThat(request.getOldValue()).isEqualTo("Old Name");
        assertThat(request.getNewValue()).isEqualTo("New Name");
        assertThat(request.getRequestedBy()).isEqualTo("user-1");
    }

    @Test
    @DisplayName("Should set and get all fields")
    void testGettersAndSetters() {
        updateRequest.setId("req-1");
        updateRequest.setUserId("user-1");
        updateRequest.setRequestType("PROFILE_UPDATE");
        updateRequest.setFieldName("name");
        updateRequest.setOldValue("Old Name");
        updateRequest.setNewValue("New Name");
        updateRequest.setOldValueEncrypted("encrypted-old");
        updateRequest.setNewValueEncrypted("encrypted-new");
        updateRequest.setStatus("APPROVED");
        updateRequest.setRequestedBy("user-1");
        updateRequest.setReviewedBy("admin-1");
        LocalDateTime now = LocalDateTime.now();
        updateRequest.setRequestedAt(now);
        updateRequest.setReviewedAt(now);
        updateRequest.setRejectionReason("Invalid");
        updateRequest.setOtpVerified(true);
        updateRequest.setOtpId("otp-1");

        assertThat(updateRequest.getId()).isEqualTo("req-1");
        assertThat(updateRequest.getUserId()).isEqualTo("user-1");
        assertThat(updateRequest.getRequestType()).isEqualTo("PROFILE_UPDATE");
        assertThat(updateRequest.getFieldName()).isEqualTo("name");
        assertThat(updateRequest.getOldValue()).isEqualTo("Old Name");
        assertThat(updateRequest.getNewValue()).isEqualTo("New Name");
        assertThat(updateRequest.getOldValueEncrypted()).isEqualTo("encrypted-old");
        assertThat(updateRequest.getNewValueEncrypted()).isEqualTo("encrypted-new");
        assertThat(updateRequest.getStatus()).isEqualTo("APPROVED");
        assertThat(updateRequest.getRequestedBy()).isEqualTo("user-1");
        assertThat(updateRequest.getReviewedBy()).isEqualTo("admin-1");
        assertThat(updateRequest.getRequestedAt()).isEqualTo(now);
        assertThat(updateRequest.getReviewedAt()).isEqualTo(now);
        assertThat(updateRequest.getRejectionReason()).isEqualTo("Invalid");
        assertThat(updateRequest.isOtpVerified()).isTrue();
        assertThat(updateRequest.getOtpId()).isEqualTo("otp-1");
    }
}

