package com.kitchensink.service;

import com.kitchensink.dto.UpdateRequestResponseDTO;
import com.kitchensink.exception.ResourceConflictException;
import com.kitchensink.exception.ResourceNotFoundException;
import com.kitchensink.model.UpdateRequest;
import com.kitchensink.model.User;
import com.kitchensink.repository.UpdateRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateRequestService Tests")
class UpdateRequestServiceTest {

    @Mock
    private UpdateRequestRepository updateRequestRepository;

    @Mock
    private UserService userService;

    @Mock
    private EmailService emailService;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private UpdateRequestService updateRequestService;

    private User testUser;
    private UpdateRequest testRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("user-1");
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPhoneNumber("9876543210");
        testUser.setIsdCode("+91");
        testUser.setDateOfBirth("01-01-1990");
        testUser.setAddress("Address");
        testUser.setCity("City");
        testUser.setCountry("Country");

        testRequest = new UpdateRequest();
        testRequest.setId("req-1");
        testRequest.setUserId("user-1");
        testRequest.setRequestType("PROFILE_UPDATE");
        testRequest.setFieldName("name");
        testRequest.setOldValue("Old Name");
        testRequest.setNewValue("New Name");
        testRequest.setStatus("PENDING");
        testRequest.setRequestedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should create update request successfully")
    void testCreateUpdateRequest_Success() {
        when(userService.getUserById("user-1")).thenReturn(testUser);
        when(encryptionService.encrypt("Test User")).thenReturn("encrypted-old");
        when(encryptionService.encrypt("New Name")).thenReturn("encrypted-new");
        when(updateRequestRepository.save(any(UpdateRequest.class))).thenReturn(testRequest);
        when(userService.getUserByEmail("rohitku860@gmail.com")).thenReturn(Optional.empty());
        doNothing().when(emailService).sendUpdateRequestNotification(anyString(), anyString(), anyString());

        UpdateRequest result = updateRequestService.createUpdateRequest("user-1", "name", "New Name");

        assertThat(result).isNotNull();
        verify(updateRequestRepository).save(any(UpdateRequest.class));
    }

    @Test
    @DisplayName("Should throw exception when field not found")
    void testCreateUpdateRequest_FieldNotFound() {
        when(userService.getUserById("user-1")).thenReturn(testUser);

        assertThatThrownBy(() -> updateRequestService.createUpdateRequest("user-1", "invalidField", "value"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should create email change request successfully")
    void testCreateEmailChangeRequest_Success() {
        when(userService.getUserById("user-1")).thenReturn(testUser);
        when(encryptionService.encrypt("test@example.com")).thenReturn("encrypted-old");
        when(encryptionService.encrypt("new@example.com")).thenReturn("encrypted-new");
        when(updateRequestRepository.save(any(UpdateRequest.class))).thenReturn(testRequest);
        when(userService.getUserByEmail("rohitku860@gmail.com")).thenReturn(Optional.empty());
        doNothing().when(emailService).sendUpdateRequestNotification(anyString(), anyString(), anyString());

        UpdateRequest result = updateRequestService.createEmailChangeRequest("user-1", "new@example.com");

        assertThat(result).isNotNull();
        verify(updateRequestRepository).save(any(UpdateRequest.class));
    }

    @Test
    @DisplayName("Should create phone number update request successfully")
    void testCreatePhoneNumberUpdateRequest_Success() {
        when(userService.getUserById("user-1")).thenReturn(testUser);
        when(encryptionService.encrypt(anyString())).thenReturn("encrypted");
        when(updateRequestRepository.save(any(UpdateRequest.class))).thenReturn(testRequest);
        when(userService.getUserByEmail("rohitku860@gmail.com")).thenReturn(Optional.empty());
        doNothing().when(emailService).sendUpdateRequestNotification(anyString(), anyString(), anyString());

        UpdateRequest result = updateRequestService.createPhoneNumberUpdateRequest("user-1", "9876543211", "+91");

        assertThat(result).isNotNull();
        verify(updateRequestRepository, atLeastOnce()).save(any(UpdateRequest.class));
    }

    @Test
    @DisplayName("Should get pending requests successfully")
    void testGetPendingRequests() {
        when(updateRequestRepository.findByStatusOrderByRequestedAtDesc("PENDING"))
                .thenReturn(Collections.singletonList(testRequest));
        when(encryptionService.decrypt("encrypted-old")).thenReturn("Old Name");
        when(encryptionService.decrypt("encrypted-new")).thenReturn("New Name");
        testRequest.setOldValueEncrypted("encrypted-old");
        testRequest.setNewValueEncrypted("encrypted-new");

        List<UpdateRequest> result = updateRequestService.getPendingRequests();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOldValue()).isEqualTo("Old Name");
    }

    @Test
    @DisplayName("Should get user requests successfully")
    void testGetUserRequests() {
        when(updateRequestRepository.findByUserIdOrderByRequestedAtDesc("user-1"))
                .thenReturn(Collections.singletonList(testRequest));
        when(encryptionService.decrypt("encrypted-old")).thenReturn("Old Name");
        when(encryptionService.decrypt("encrypted-new")).thenReturn("New Name");
        testRequest.setOldValueEncrypted("encrypted-old");
        testRequest.setNewValueEncrypted("encrypted-new");

        List<UpdateRequest> result = updateRequestService.getUserRequests("user-1");

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Should approve request successfully")
    void testApproveRequest_Success() {
        testRequest.setOldValueEncrypted("encrypted-old");
        testRequest.setNewValueEncrypted("encrypted-new");
        when(updateRequestRepository.findById("req-1")).thenReturn(Optional.of(testRequest));
        when(encryptionService.decrypt("encrypted-old")).thenReturn("Old Name");
        when(encryptionService.decrypt("encrypted-new")).thenReturn("New Name");
        when(userService.getUserById("user-1")).thenReturn(testUser);
        when(updateRequestRepository.findByUserIdAndFieldNameAndStatus("user-1", "isdCode", "PENDING"))
                .thenReturn(Optional.empty());
        doNothing().when(userService).updateUser(anyString(), anyString(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
        when(updateRequestRepository.save(any(UpdateRequest.class))).thenReturn(testRequest);
        doNothing().when(emailService).sendUpdateRequestApproval(anyString(), anyString(), anyString());
        doNothing().when(auditService).logUpdateRequestApproved(anyString(), anyString(), anyString(), anyString());

        UpdateRequestResponseDTO result = updateRequestService.approveRequest("req-1", "admin-1");

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("APPROVED");
        verify(updateRequestRepository).save(any(UpdateRequest.class));
    }

    @Test
    @DisplayName("Should reject request successfully")
    void testRejectRequest_Success() {
        testRequest.setNewValueEncrypted("encrypted-new");
        when(updateRequestRepository.findById("req-1")).thenReturn(Optional.of(testRequest));
        when(encryptionService.decrypt("encrypted-new")).thenReturn("New Name");
        when(updateRequestRepository.save(any(UpdateRequest.class))).thenReturn(testRequest);
        when(userService.getUserById("user-1")).thenReturn(testUser);
        doNothing().when(emailService).sendUpdateRequestRejection(anyString(), anyString(), anyString(), anyString());
        doNothing().when(auditService).logUpdateRequestRejected(anyString(), anyString(), anyString(), anyString(), anyString());

        UpdateRequestResponseDTO result = updateRequestService.rejectRequest("req-1", "admin-1", "Invalid request");

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("REJECTED");
        verify(updateRequestRepository).save(any(UpdateRequest.class));
    }

    @Test
    @DisplayName("Should revoke request successfully")
    void testRevokeRequest_Success() {
        when(updateRequestRepository.findById("req-1")).thenReturn(Optional.of(testRequest));
        doNothing().when(updateRequestRepository).delete(testRequest);
        doNothing().when(auditService).logUpdateRequestRevoked(anyString(), anyString(), anyString());

        updateRequestService.revokeRequest("req-1", "user-1");

        verify(updateRequestRepository).delete(testRequest);
        verify(auditService).logUpdateRequestRevoked("req-1", "user-1", "name");
    }

    @Test
    @DisplayName("Should throw exception when revoking other user's request")
    void testRevokeRequest_DifferentUser() {
        when(updateRequestRepository.findById("req-1")).thenReturn(Optional.of(testRequest));

        assertThatThrownBy(() -> updateRequestService.revokeRequest("req-1", "other-user"))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("You can only revoke your own");
    }

    @Test
    @DisplayName("Should throw exception when revoking non-pending request")
    void testRevokeRequest_NonPending() {
        testRequest.setStatus("APPROVED");
        when(updateRequestRepository.findById("req-1")).thenReturn(Optional.of(testRequest));

        assertThatThrownBy(() -> updateRequestService.revokeRequest("req-1", "user-1"))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Only pending requests");
    }

    @Test
    @DisplayName("Should map update request to DTO successfully")
    void testMapToUpdateRequestDTO() {
        testRequest.setOldValueEncrypted("encrypted-old");
        testRequest.setNewValueEncrypted("encrypted-new");
        when(encryptionService.decrypt("encrypted-old")).thenReturn("Old Name");
        when(encryptionService.decrypt("encrypted-new")).thenReturn("New Name");

        List<UpdateRequestResponseDTO> result = updateRequestService.mapToUpdateRequestDTOs(Collections.singletonList(testRequest));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("req-1");
        assertThat(result.get(0).getOldValue()).isEqualTo("Old Name");
        assertThat(result.get(0).getNewValue()).isEqualTo("New Name");
    }
}

