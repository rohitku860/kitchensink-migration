package com.kitchensink.controller;

import com.kitchensink.dto.Response;
import com.kitchensink.dto.UpdateRequestResponseDTO;
import com.kitchensink.dto.UserRequestDTO;
import com.kitchensink.dto.UserResponseDTO;
import com.kitchensink.model.UpdateRequest;
import com.kitchensink.model.User;
import com.kitchensink.service.EmailService;
import com.kitchensink.service.RoleService;
import com.kitchensink.service.UpdateRequestService;
import com.kitchensink.service.UserService;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminController Tests")
class AdminControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private UpdateRequestService updateRequestService;

    @Mock
    private EmailService emailService;

    @Mock
    private RoleService roleService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AdminController adminController;

    private User testUser;
    private UserRequestDTO userRequestDTO;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("user-1");
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setIsdCode("+91");
        testUser.setPhoneNumber("9876543210");
        testUser.setDateOfBirth("01-01-1990");
        testUser.setAddress("Test Address");
        testUser.setCity("Test City");
        testUser.setCountry("India");
        testUser.setRegistrationDate(LocalDateTime.now());
        testUser.setStatus("ACTIVE");

        userRequestDTO = new UserRequestDTO();
        userRequestDTO.setName("New User");
        userRequestDTO.setEmail("newuser@example.com");
        userRequestDTO.setIsdCode("+91");
        userRequestDTO.setPhoneNumber("9876543211");
        userRequestDTO.setDateOfBirth("01-01-1995");
        userRequestDTO.setAddress("New Address");
        userRequestDTO.setCity("New City");
        userRequestDTO.setCountry("India");
    }

    @Test
    @DisplayName("Should get all users successfully with default parameters")
    void testGetAllUsers() {
        com.kitchensink.dto.CursorPageResponse<User> cursorPage = 
                new com.kitchensink.dto.CursorPageResponse<>(
                        Collections.singletonList(testUser),
                        "user-1",
                        null,
                        false,
                        false,
                        1
                );
        when(userService.getAllUsersExcludingAdminsCursor(null, 10, com.kitchensink.enums.Direction.NEXT))
                .thenReturn(cursorPage);
        when(roleService.getRoleNameByUserId(anyString())).thenReturn("USER");

        ResponseEntity<Response<com.kitchensink.dto.CursorPageResponse<UserResponseDTO>>> response = 
                adminController.getAllUsers(10, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).isNotNull();
        verify(userService).getAllUsersExcludingAdminsCursor(null, 10, com.kitchensink.enums.Direction.NEXT);
    }

    @Test
    @DisplayName("Should get all users with cursor pagination - first page")
    void testGetAllUsers_CursorFirstPage() {
        com.kitchensink.dto.CursorPageResponse<User> cursorPage = 
                new com.kitchensink.dto.CursorPageResponse<>(
                        Collections.singletonList(testUser),
                        "user-1",
                        null,
                        true,
                        false,
                        1
                );
        when(userService.getAllUsersExcludingAdminsCursor(null, 10, com.kitchensink.enums.Direction.NEXT))
                .thenReturn(cursorPage);
        when(roleService.getRoleNameByUserId(anyString())).thenReturn("USER");

        ResponseEntity<Response<com.kitchensink.dto.CursorPageResponse<UserResponseDTO>>> response = 
                adminController.getAllUsers(10, null, "next");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).isNotNull();
        verify(userService).getAllUsersExcludingAdminsCursor(null, 10, com.kitchensink.enums.Direction.NEXT);
    }

    @Test
    @DisplayName("Should get all users with cursor pagination - next page")
    void testGetAllUsers_CursorNextPage() {
        com.kitchensink.dto.CursorPageResponse<User> cursorPage = 
                new com.kitchensink.dto.CursorPageResponse<>(
                        Collections.singletonList(testUser),
                        "user-2",
                        "user-0",
                        true,
                        true,
                        1
                );
        when(userService.getAllUsersExcludingAdminsCursor("user-0", 10, com.kitchensink.enums.Direction.NEXT))
                .thenReturn(cursorPage);
        when(roleService.getRoleNameByUserId(anyString())).thenReturn("USER");

        ResponseEntity<Response<com.kitchensink.dto.CursorPageResponse<UserResponseDTO>>> response = 
                adminController.getAllUsers(10, "user-0", "next");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).isNotNull();
        verify(userService).getAllUsersExcludingAdminsCursor("user-0", 10, com.kitchensink.enums.Direction.NEXT);
    }

    @Test
    @DisplayName("Should get all users with cursor pagination - previous page")
    void testGetAllUsers_CursorPreviousPage() {
        com.kitchensink.dto.CursorPageResponse<User> cursorPage = 
                new com.kitchensink.dto.CursorPageResponse<>(
                        Collections.singletonList(testUser),
                        "user-2",
                        "user-0",
                        true,
                        true,
                        1
                );
        when(userService.getAllUsersExcludingAdminsCursor("user-2", 10, com.kitchensink.enums.Direction.PREV))
                .thenReturn(cursorPage);
        when(roleService.getRoleNameByUserId(anyString())).thenReturn("USER");

        ResponseEntity<Response<com.kitchensink.dto.CursorPageResponse<UserResponseDTO>>> response = 
                adminController.getAllUsers(10, "user-2", "previous");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).isNotNull();
        verify(userService).getAllUsersExcludingAdminsCursor("user-2", 10, com.kitchensink.enums.Direction.PREV);
    }

    @Test
    @DisplayName("Should get all users with custom size parameter")
    void testGetAllUsers_CustomSize() {
        com.kitchensink.dto.CursorPageResponse<User> cursorPage = 
                new com.kitchensink.dto.CursorPageResponse<>(
                        Collections.singletonList(testUser),
                        "user-1",
                        null,
                        false,
                        false,
                        1
                );
        when(userService.getAllUsersExcludingAdminsCursor(null, 20, com.kitchensink.enums.Direction.NEXT))
                .thenReturn(cursorPage);
        when(roleService.getRoleNameByUserId(anyString())).thenReturn("USER");

        ResponseEntity<Response<com.kitchensink.dto.CursorPageResponse<UserResponseDTO>>> response = 
                adminController.getAllUsers(20, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        verify(userService).getAllUsersExcludingAdminsCursor(null, 20, com.kitchensink.enums.Direction.NEXT);
    }

    @Test
    @DisplayName("Should search users successfully")
    void testSearchUsers() {
        when(userService.searchUsersByNameExcludingAdmins("Test")).thenReturn(Collections.singletonList(testUser));
        when(roleService.getRoleNameByUserId(anyString())).thenReturn("USER");

        ResponseEntity<Response<List<UserResponseDTO>>> response = adminController.searchUsers("Test");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).hasSize(1);
        verify(userService).searchUsersByNameExcludingAdmins("Test");
    }

    @Test
    @DisplayName("Should create user successfully")
    void testCreateUser() {
        when(userService.createUser(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString())).thenReturn(testUser);
        when(roleService.getRoleNameByUserId(anyString())).thenReturn("USER");
        doNothing().when(emailService).sendUserCreationEmail(anyString(), anyString());

        ResponseEntity<Response<UserResponseDTO>> response = adminController.createUser(userRequestDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        verify(userService).createUser(eq(userRequestDTO.getName()), eq(userRequestDTO.getEmail()),
                eq(userRequestDTO.getIsdCode()), eq(userRequestDTO.getPhoneNumber()), eq("USER"),
                eq(userRequestDTO.getDateOfBirth()), eq(userRequestDTO.getAddress()),
                eq(userRequestDTO.getCity()), eq(userRequestDTO.getCountry()));
        verify(emailService).sendUserCreationEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Should update user successfully")
    void testUpdateUser() {
        when(userService.updateUser(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString())).thenReturn(testUser);
        when(roleService.getRoleNameByUserId(anyString())).thenReturn("USER");
        doNothing().when(emailService).sendUserUpdateNotification(anyString(), anyString());

        ResponseEntity<Response<UserResponseDTO>> response = adminController.updateUser("user-1", userRequestDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        verify(userService).updateUser(eq("user-1"), eq(userRequestDTO.getName()), eq(userRequestDTO.getEmail()),
                eq(userRequestDTO.getIsdCode()), eq(userRequestDTO.getPhoneNumber()),
                eq(userRequestDTO.getDateOfBirth()), eq(userRequestDTO.getAddress()),
                eq(userRequestDTO.getCity()), eq(userRequestDTO.getCountry()));
        verify(emailService).sendUserUpdateNotification(anyString(), anyString());
    }

    @Test
    @DisplayName("Should delete user successfully")
    void testDeleteUser() {
        when(userService.getUserById("user-1")).thenReturn(testUser);
        doNothing().when(userService).deleteUser("user-1");
        doNothing().when(emailService).sendUserDeletionNotification(anyString(), anyString());

        ResponseEntity<Response<Void>> response = adminController.deleteUser("user-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        verify(userService).deleteUser("user-1");
        verify(emailService).sendUserDeletionNotification(anyString(), anyString());
    }

    @Test
    @DisplayName("Should get pending update requests successfully")
    void testGetPendingUpdateRequests() {
        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.setId("req-1");
        updateRequest.setUserId("user-1");
        updateRequest.setFieldName("name");
        updateRequest.setStatus("PENDING");

        UpdateRequestResponseDTO dto = new UpdateRequestResponseDTO();
        dto.setId("req-1");
        dto.setUserId("user-1");
        dto.setFieldName("name");
        dto.setStatus("PENDING");

        when(updateRequestService.getPendingRequests()).thenReturn(Collections.singletonList(updateRequest));
        when(updateRequestService.mapToUpdateRequestDTOs(anyList())).thenReturn(Collections.singletonList(dto));

        ResponseEntity<Response<List<UpdateRequestResponseDTO>>> response = adminController.getPendingUpdateRequests();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).hasSize(1);
        verify(updateRequestService).getPendingRequests();
    }

    @Test
    @DisplayName("Should approve update request successfully")
    void testApproveUpdateRequest() {
        when(authentication.getName()).thenReturn("admin-1");
        UpdateRequestResponseDTO dto = new UpdateRequestResponseDTO();
        dto.setId("req-1");
        dto.setStatus("APPROVED");
        when(updateRequestService.approveRequest("req-1", "admin-1")).thenReturn(dto);

        ResponseEntity<Response<UpdateRequestResponseDTO>> response = 
                adminController.approveUpdateRequest("req-1", authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        verify(updateRequestService).approveRequest("req-1", "admin-1");
    }

    @Test
    @DisplayName("Should reject update request successfully")
    void testRejectUpdateRequest() {
        when(authentication.getName()).thenReturn("admin-1");
        UpdateRequestResponseDTO dto = new UpdateRequestResponseDTO();
        dto.setId("req-1");
        dto.setStatus("REJECTED");
        Map<String, String> request = Map.of("reason", "Invalid request");
        when(updateRequestService.rejectRequest("req-1", "admin-1", "Invalid request")).thenReturn(dto);

        ResponseEntity<Response<UpdateRequestResponseDTO>> response = 
                adminController.rejectUpdateRequest("req-1", request, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        verify(updateRequestService).rejectRequest("req-1", "admin-1", "Invalid request");
    }

    @Test
    @DisplayName("Should reject update request with default reason when not provided")
    void testRejectUpdateRequest_NoReason() {
        when(authentication.getName()).thenReturn("admin-1");
        UpdateRequestResponseDTO dto = new UpdateRequestResponseDTO();
        dto.setId("req-1");
        dto.setStatus("REJECTED");
        Map<String, String> request = Collections.emptyMap();
        when(updateRequestService.rejectRequest("req-1", "admin-1", "No reason provided")).thenReturn(dto);

        ResponseEntity<Response<UpdateRequestResponseDTO>> response = 
                adminController.rejectUpdateRequest("req-1", request, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(updateRequestService).rejectRequest("req-1", "admin-1", "No reason provided");
    }

    // Cross-role access tests
    // Note: These tests verify business logic. For full security annotation testing (@PreAuthorize),
    // integration tests with @WithMockUser or @WithUserDetails would be needed.

    @Test
    @DisplayName("Should allow admin to access all users endpoint")
    void testGetAllUsers_AdminAccess() {
        com.kitchensink.dto.CursorPageResponse<User> cursorPage = 
                new com.kitchensink.dto.CursorPageResponse<>(
                        Collections.singletonList(testUser),
                        "user-1",
                        null,
                        false,
                        false,
                        1
                );
        when(userService.getAllUsersExcludingAdminsCursor(null, 10, com.kitchensink.enums.Direction.NEXT))
                .thenReturn(cursorPage);
        when(roleService.getRoleNameByUserId(anyString())).thenReturn("USER");

        ResponseEntity<Response<com.kitchensink.dto.CursorPageResponse<UserResponseDTO>>> response = 
                adminController.getAllUsers(10, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        // Admin can access - verified by successful response
    }

    @Test
    @DisplayName("Should allow admin to create users")
    void testCreateUser_AdminAccess() {
        when(userService.createUser(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString())).thenReturn(testUser);
        when(roleService.getRoleNameByUserId(anyString())).thenReturn("USER");
        doNothing().when(emailService).sendUserCreationEmail(anyString(), anyString());

        ResponseEntity<Response<UserResponseDTO>> response = adminController.createUser(userRequestDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        // Admin can create users - verified by successful response
    }

    @Test
    @DisplayName("Should allow admin to update any user")
    void testUpdateUser_AdminAccess() {
        when(userService.updateUser(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString())).thenReturn(testUser);
        when(roleService.getRoleNameByUserId(anyString())).thenReturn("USER");
        doNothing().when(emailService).sendUserUpdateNotification(anyString(), anyString());

        ResponseEntity<Response<UserResponseDTO>> response = adminController.updateUser("user-1", userRequestDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        // Admin can update any user - verified by successful response
    }

    @Test
    @DisplayName("Should allow admin to delete any user")
    void testDeleteUser_AdminAccess() {
        when(userService.getUserById("user-1")).thenReturn(testUser);
        doNothing().when(userService).deleteUser("user-1");
        doNothing().when(emailService).sendUserDeletionNotification(anyString(), anyString());

        ResponseEntity<Response<Void>> response = adminController.deleteUser("user-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        // Admin can delete any user - verified by successful response
    }
}

