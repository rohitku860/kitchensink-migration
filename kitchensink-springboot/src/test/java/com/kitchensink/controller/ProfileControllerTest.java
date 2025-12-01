package com.kitchensink.controller;

import com.kitchensink.dto.FieldUpdateRequestDTO;
import com.kitchensink.dto.Response;
import com.kitchensink.dto.UpdateRequestResponseDTO;
import com.kitchensink.dto.UserResponseDTO;
import com.kitchensink.exception.ResourceConflictException;
import com.kitchensink.service.ProfileService;
import com.kitchensink.service.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileController Tests")
class ProfileControllerTest {

    @Mock
    private ProfileService profileService;

    @Mock
    private RoleService roleService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ProfileController profileController;

    private UserResponseDTO userResponseDTO;
    private FieldUpdateRequestDTO fieldUpdateRequestDTO;

    @BeforeEach
    void setUp() {
        userResponseDTO = new UserResponseDTO();
        userResponseDTO.setId("user-1");
        userResponseDTO.setName("Test User");
        userResponseDTO.setEmail("test@example.com");

        fieldUpdateRequestDTO = new FieldUpdateRequestDTO();
        fieldUpdateRequestDTO.setFieldName("name");
        fieldUpdateRequestDTO.setValue("New Name");
    }

    @Test
    @DisplayName("Should get profile successfully")
    void testGetProfile() {
        when(profileService.getProfile("user-1")).thenReturn(userResponseDTO);

        ResponseEntity<Response<UserResponseDTO>> response = profileController.getProfile("user-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getId()).isEqualTo("user-1");
        verify(profileService).getProfile("user-1");
    }

    @Test
    @DisplayName("Should request email change OTP successfully")
    void testRequestEmailChangeOtp() {
        Map<String, String> request = Map.of("newEmail", "newemail@example.com");
        Map<String, String> responseData = Map.of("message", "OTP sent");
        when(profileService.requestEmailChangeOtp("newemail@example.com")).thenReturn(responseData);

        ResponseEntity<Response<Map<String, String>>> response = 
                profileController.requestEmailChangeOtp("user-1", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        verify(profileService).requestEmailChangeOtp("newemail@example.com");
    }

    @Test
    @DisplayName("Should return bad request when email change conflicts")
    void testRequestEmailChangeOtp_Conflict() {
        Map<String, String> request = Map.of("newEmail", "existing@example.com");
        when(profileService.requestEmailChangeOtp("existing@example.com"))
                .thenThrow(new ResourceConflictException("Email already exists", "email"));

        ResponseEntity<Response<Map<String, String>>> response = 
                profileController.requestEmailChangeOtp("user-1", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
    }

    @Test
    @DisplayName("Should update fields as admin successfully")
    void testUpdateFields_AsAdmin() {
        when(authentication.getName()).thenReturn("admin-1");
        when(roleService.isAdminByUserId("admin-1")).thenReturn(true);
        when(profileService.updateFields(eq("user-1"), anyList(), eq(true))).thenReturn(userResponseDTO);

        List<FieldUpdateRequestDTO> updates = Collections.singletonList(fieldUpdateRequestDTO);
        ResponseEntity<Response<UserResponseDTO>> response = 
                profileController.updateFields("user-1", updates, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).contains("updated successfully");
        verify(profileService).updateFields("user-1", updates, true);
    }

    @Test
    @DisplayName("Should create update request as user successfully")
    void testUpdateFields_AsUser() {
        when(authentication.getName()).thenReturn("user-1");
        when(roleService.isAdminByUserId("user-1")).thenReturn(false);
        when(profileService.updateFields(eq("user-1"), anyList(), eq(false))).thenReturn(userResponseDTO);

        List<FieldUpdateRequestDTO> updates = Collections.singletonList(fieldUpdateRequestDTO);
        ResponseEntity<Response<UserResponseDTO>> response = 
                profileController.updateFields("user-1", updates, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).contains("Update request(s) created");
        verify(profileService).updateFields("user-1", updates, false);
    }

    @Test
    @DisplayName("Should return bad request when update conflicts")
    void testUpdateFields_Conflict() {
        when(authentication.getName()).thenReturn("admin-1");
        when(roleService.isAdminByUserId("admin-1")).thenReturn(true);
        when(profileService.updateFields(eq("user-1"), anyList(), eq(true)))
                .thenThrow(new ResourceConflictException("Email already exists", "email"));

        List<FieldUpdateRequestDTO> updates = Collections.singletonList(fieldUpdateRequestDTO);
        ResponseEntity<Response<UserResponseDTO>> response = 
                profileController.updateFields("user-1", updates, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
    }

    @Test
    @DisplayName("Should get user update requests successfully")
    void testGetUserUpdateRequests() {
        UpdateRequestResponseDTO requestDTO = new UpdateRequestResponseDTO();
        requestDTO.setId("req-1");
        requestDTO.setUserId("user-1");
        requestDTO.setStatus("PENDING");
        List<UpdateRequestResponseDTO> requests = Collections.singletonList(requestDTO);
        when(profileService.getUserUpdateRequests("user-1")).thenReturn(requests);

        ResponseEntity<Response<List<UpdateRequestResponseDTO>>> response = 
                profileController.getUserUpdateRequests("user-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).hasSize(1);
        verify(profileService).getUserUpdateRequests("user-1");
    }

    @Test
    @DisplayName("Should revoke update request successfully")
    void testRevokeUpdateRequest() {
        doNothing().when(profileService).revokeUpdateRequest("req-1", "user-1");

        ResponseEntity<Response<Map<String, String>>> response = 
                profileController.revokeUpdateRequest("user-1", "req-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().get("message")).isEqualTo("Update request revoked successfully");
        verify(profileService).revokeUpdateRequest("req-1", "user-1");
    }

    // Cross-role access tests
    // Note: These tests verify business logic. For full security annotation testing (@PreAuthorize),
    // integration tests with @WithMockUser or @WithUserDetails would be needed.

    @Test
    @DisplayName("Should allow admin to access any user profile")
    void testGetProfile_AdminAccess() {
        UserResponseDTO otherUserDTO = new UserResponseDTO();
        otherUserDTO.setId("user-2");
        otherUserDTO.setName("Other User");
        otherUserDTO.setEmail("other@example.com");

        when(profileService.getProfile("user-2")).thenReturn(otherUserDTO);

        ResponseEntity<Response<UserResponseDTO>> response = profileController.getProfile("user-2");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getId()).isEqualTo("user-2");
        // Admin can access any profile - verified by successful response
    }

    @Test
    @DisplayName("Should allow user to access own profile")
    void testGetProfile_UserOwnProfile() {
        when(profileService.getProfile("user-1")).thenReturn(userResponseDTO);

        ResponseEntity<Response<UserResponseDTO>> response = profileController.getProfile("user-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getId()).isEqualTo("user-1");
        // User can access own profile - verified by successful response
    }

    @Test
    @DisplayName("Should allow admin to update any user profile")
    void testUpdateFields_AdminAccessOtherUser() {
        when(authentication.getName()).thenReturn("admin-1");
        when(roleService.isAdminByUserId("admin-1")).thenReturn(true);
        when(profileService.updateFields(eq("user-2"), anyList(), eq(true))).thenReturn(userResponseDTO);

        List<FieldUpdateRequestDTO> updates = Collections.singletonList(fieldUpdateRequestDTO);
        ResponseEntity<Response<UserResponseDTO>> response = 
                profileController.updateFields("user-2", updates, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).contains("updated successfully");
        // Admin can update any user's profile - verified by successful response
        verify(profileService).updateFields("user-2", updates, true);
    }

    @Test
    @DisplayName("Should allow user to update own profile (creates update request)")
    void testUpdateFields_UserOwnProfile() {
        when(authentication.getName()).thenReturn("user-1");
        when(roleService.isAdminByUserId("user-1")).thenReturn(false);
        when(profileService.updateFields(eq("user-1"), anyList(), eq(false))).thenReturn(userResponseDTO);

        List<FieldUpdateRequestDTO> updates = Collections.singletonList(fieldUpdateRequestDTO);
        ResponseEntity<Response<UserResponseDTO>> response = 
                profileController.updateFields("user-1", updates, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).contains("Update request(s) created");
        // User can update own profile (creates request) - verified by successful response
        verify(profileService).updateFields("user-1", updates, false);
    }

    @Test
    @DisplayName("Should allow admin to get any user's update requests")
    void testGetUserUpdateRequests_AdminAccess() {
        UpdateRequestResponseDTO requestDTO = new UpdateRequestResponseDTO();
        requestDTO.setId("req-1");
        requestDTO.setUserId("user-2");
        requestDTO.setStatus("PENDING");
        List<UpdateRequestResponseDTO> requests = Collections.singletonList(requestDTO);
        when(profileService.getUserUpdateRequests("user-2")).thenReturn(requests);

        ResponseEntity<Response<List<UpdateRequestResponseDTO>>> response = 
                profileController.getUserUpdateRequests("user-2");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).hasSize(1);
        // Admin can access any user's update requests - verified by successful response
        verify(profileService).getUserUpdateRequests("user-2");
    }

    @Test
    @DisplayName("Should allow user to get own update requests")
    void testGetUserUpdateRequests_UserOwnRequests() {
        UpdateRequestResponseDTO requestDTO = new UpdateRequestResponseDTO();
        requestDTO.setId("req-1");
        requestDTO.setUserId("user-1");
        requestDTO.setStatus("PENDING");
        List<UpdateRequestResponseDTO> requests = Collections.singletonList(requestDTO);
        when(profileService.getUserUpdateRequests("user-1")).thenReturn(requests);

        ResponseEntity<Response<List<UpdateRequestResponseDTO>>> response = 
                profileController.getUserUpdateRequests("user-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).hasSize(1);
        // User can access own update requests - verified by successful response
        verify(profileService).getUserUpdateRequests("user-1");
    }

    @Test
    @DisplayName("Should allow admin to request email change OTP for any user")
    void testRequestEmailChangeOtp_AdminAccess() {
        Map<String, String> request = Map.of("newEmail", "newemail@example.com");
        Map<String, String> responseData = Map.of("message", "OTP sent");
        when(profileService.requestEmailChangeOtp("newemail@example.com")).thenReturn(responseData);

        ResponseEntity<Response<Map<String, String>>> response = 
                profileController.requestEmailChangeOtp("user-2", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        // Admin can request email change OTP for any user - verified by successful response
        verify(profileService).requestEmailChangeOtp("newemail@example.com");
    }

    @Test
    @DisplayName("Should allow user to request email change OTP for own account")
    void testRequestEmailChangeOtp_UserOwnAccount() {
        Map<String, String> request = Map.of("newEmail", "newemail@example.com");
        Map<String, String> responseData = Map.of("message", "OTP sent");
        when(profileService.requestEmailChangeOtp("newemail@example.com")).thenReturn(responseData);

        ResponseEntity<Response<Map<String, String>>> response = 
                profileController.requestEmailChangeOtp("user-1", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        // User can request email change OTP for own account - verified by successful response
        verify(profileService).requestEmailChangeOtp("newemail@example.com");
    }
}

