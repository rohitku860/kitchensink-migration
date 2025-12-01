package com.kitchensink.service;

import com.kitchensink.dto.FieldUpdateRequestDTO;
import com.kitchensink.dto.UpdateRequestResponseDTO;
import com.kitchensink.dto.UserResponseDTO;
import com.kitchensink.exception.ResourceConflictException;
import com.kitchensink.model.Otp;
import com.kitchensink.model.UpdateRequest;
import com.kitchensink.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileService Tests")
class ProfileServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private UpdateRequestService updateRequestService;

    @Mock
    private OtpService otpService;

    @Mock
    private EmailService emailService;

    @Mock
    private RoleService roleService;

    @InjectMocks
    private ProfileService profileService;

    private User testUser;
    private FieldUpdateRequestDTO fieldUpdate;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("user-1");
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setIsdCode("+91");
        testUser.setPhoneNumber("9876543210");
        testUser.setDateOfBirth("01-01-1990");
        testUser.setAddress("Address");
        testUser.setCity("City");
        testUser.setCountry("Country");

        fieldUpdate = new FieldUpdateRequestDTO();
        fieldUpdate.setFieldName("name");
        fieldUpdate.setValue("New Name");
    }

    @Test
    @DisplayName("Should get profile successfully")
    void testGetProfile() {
        when(userService.getUserById("user-1")).thenReturn(testUser);
        when(roleService.getRoleNameByUserId("user-1")).thenReturn("USER");

        UserResponseDTO result = profileService.getProfile("user-1");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("user-1");
        assertThat(result.getName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("Should update fields as admin successfully")
    void testUpdateFields_AsAdmin() {
        when(userService.getUserById("user-1")).thenReturn(testUser);
        when(userService.updateUser(eq("user-1"), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString())).thenReturn(testUser);
        when(roleService.getRoleNameByUserId("user-1")).thenReturn("USER");
        doNothing().when(emailService).sendEmailChangeConfirmation(anyString(), anyString());

        List<FieldUpdateRequestDTO> updates = Collections.singletonList(fieldUpdate);
        UserResponseDTO result = profileService.updateFields("user-1", updates, true);

        assertThat(result).isNotNull();
        verify(userService).updateUser(eq("user-1"), eq("New Name"), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull());
    }

    @Test
    @DisplayName("Should create update request as user successfully")
    void testUpdateFields_AsUser() {
        when(userService.getUserById("user-1")).thenReturn(testUser);
        when(updateRequestService.createUpdateRequest("user-1", "name", "New Name"))
                .thenReturn(new UpdateRequest());

        List<FieldUpdateRequestDTO> updates = Collections.singletonList(fieldUpdate);
        UserResponseDTO result = profileService.updateFields("user-1", updates, false);

        assertThat(result).isNull();
        verify(updateRequestService).createUpdateRequest("user-1", "name", "New Name");
    }

    @Test
    @DisplayName("Should throw exception when field updates are empty")
    void testUpdateFields_EmptyUpdates() {
        assertThatThrownBy(() -> profileService.updateFields("user-1", Collections.emptyList(), true))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("At least one field update");
    }

    @Test
    @DisplayName("Should throw exception when field name is missing")
    void testUpdateFields_MissingFieldName() {
        fieldUpdate.setFieldName(null);
        List<FieldUpdateRequestDTO> updates = Collections.singletonList(fieldUpdate);

        assertThatThrownBy(() -> profileService.updateFields("user-1", updates, true))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Field name is required");
    }

    @Test
    @DisplayName("Should throw exception when value is missing")
    void testUpdateFields_MissingValue() {
        fieldUpdate.setValue(null);
        List<FieldUpdateRequestDTO> updates = Collections.singletonList(fieldUpdate);

        assertThatThrownBy(() -> profileService.updateFields("user-1", updates, true))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("New value is required");
    }

    @Test
    @DisplayName("Should request email change OTP successfully")
    void testRequestEmailChangeOtp() {
        Otp otp = new Otp();
        otp.setId("otp-1");
        when(otpService.createOtp("new@example.com", "EMAIL_CHANGE")).thenReturn(otp);
        doNothing().when(emailService).sendEmailChangeOtp(anyString(), anyString());

        Map<String, String> result = profileService.requestEmailChangeOtp("new@example.com");

        assertThat(result).isNotNull();
        assertThat(result.get("message")).isEqualTo("OTP sent to new email");
        verify(otpService).createOtp("new@example.com", "EMAIL_CHANGE");
    }

    @Test
    @DisplayName("Should throw exception when new email is empty")
    void testRequestEmailChangeOtp_EmptyEmail() {
        assertThatThrownBy(() -> profileService.requestEmailChangeOtp(""))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("New email is required");
    }

    @Test
    @DisplayName("Should get user update requests successfully")
    void testGetUserUpdateRequests() {
        UpdateRequest request = new UpdateRequest();
        request.setId("req-1");
        UpdateRequestResponseDTO dto = new UpdateRequestResponseDTO();
        dto.setId("req-1");
        when(updateRequestService.getUserRequests("user-1")).thenReturn(Collections.singletonList(request));
        when(updateRequestService.mapToUpdateRequestDTOs(anyList())).thenReturn(Collections.singletonList(dto));

        List<UpdateRequestResponseDTO> result = profileService.getUserUpdateRequests("user-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("req-1");
    }

    @Test
    @DisplayName("Should revoke update request successfully")
    void testRevokeUpdateRequest() {
        doNothing().when(updateRequestService).revokeRequest("req-1", "user-1");

        profileService.revokeUpdateRequest("req-1", "user-1");

        verify(updateRequestService).revokeRequest("req-1", "user-1");
    }

    @Test
    @DisplayName("Should handle email update with OTP verification")
    void testUpdateFields_EmailUpdateWithOtp() {
        FieldUpdateRequestDTO emailUpdate = new FieldUpdateRequestDTO();
        emailUpdate.setFieldName("email");
        emailUpdate.setValue("new@example.com");
        emailUpdate.setOtp("123456");

        when(userService.getUserById("user-1")).thenReturn(testUser);
        when(otpService.verifyOtp("new@example.com", "123456", "EMAIL_CHANGE")).thenReturn(true);
        when(userService.updateUser(eq("user-1"), isNull(), eq("new@example.com"), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull())).thenReturn(testUser);
        when(roleService.getRoleNameByUserId("user-1")).thenReturn("USER");
        doNothing().when(emailService).sendEmailChangeConfirmation(anyString(), anyString());

        List<FieldUpdateRequestDTO> updates = Collections.singletonList(emailUpdate);
        UserResponseDTO result = profileService.updateFields("user-1", updates, true);

        assertThat(result).isNotNull();
        verify(otpService).verifyOtp("new@example.com", "123456", "EMAIL_CHANGE");
    }

    @Test
    @DisplayName("Should throw exception when OTP is missing for email update")
    void testUpdateFields_EmailUpdateWithoutOtp() {
        FieldUpdateRequestDTO emailUpdate = new FieldUpdateRequestDTO();
        emailUpdate.setFieldName("email");
        emailUpdate.setValue("new@example.com");
        emailUpdate.setOtp(null);

        when(userService.getUserById("user-1")).thenReturn(testUser);

        List<FieldUpdateRequestDTO> updates = Collections.singletonList(emailUpdate);
        assertThatThrownBy(() -> profileService.updateFields("user-1", updates, true))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("OTP is required");
    }

    @Test
    @DisplayName("Should handle phone number update with ISD code")
    void testUpdateFields_PhoneUpdateWithIsdCode() {
        FieldUpdateRequestDTO phoneUpdate = new FieldUpdateRequestDTO();
        phoneUpdate.setFieldName("phoneNumber");
        phoneUpdate.setValue("9876543211");
        phoneUpdate.setIsdCode("+91");

        when(userService.getUserById("user-1")).thenReturn(testUser);
        when(userService.updateUser(eq("user-1"), isNull(), isNull(), eq("+91"), eq("9876543211"),
                isNull(), isNull(), isNull(), isNull())).thenReturn(testUser);
        when(roleService.getRoleNameByUserId("user-1")).thenReturn("USER");

        List<FieldUpdateRequestDTO> updates = Collections.singletonList(phoneUpdate);
        UserResponseDTO result = profileService.updateFields("user-1", updates, true);

        assertThat(result).isNotNull();
        verify(userService).updateUser(eq("user-1"), isNull(), isNull(), eq("+91"), eq("9876543211"),
                isNull(), isNull(), isNull(), isNull());
    }
}

