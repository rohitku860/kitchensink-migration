package com.kitchensink.service;

import com.kitchensink.model.UserRole;
import com.kitchensink.repository.UserRoleRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRoleService Tests")
class UserRoleServiceTest {

    @Mock
    private UserRoleRepository userRoleRepository;

    @InjectMocks
    private UserRoleService userRoleService;

    private UserRole testUserRole;

    @BeforeEach
    void setUp() {
        testUserRole = new UserRole();
        testUserRole.setId("ur-1");
        testUserRole.setUserId("user-1");
        testUserRole.setRoleId("role-1");
        testUserRole.setActive(true);
        testUserRole.setAssignedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should assign role to user successfully")
    void testAssignRoleToUser_NewAssignment() {
        when(userRoleRepository.findByUserId("user-1")).thenReturn(Optional.empty());
        when(userRoleRepository.save(any(UserRole.class))).thenReturn(testUserRole);

        UserRole result = userRoleService.assignRoleToUser("user-1", "role-1");

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo("user-1");
        assertThat(result.getRoleId()).isEqualTo("role-1");
        verify(userRoleRepository).save(any(UserRole.class));
    }

    @Test
    @DisplayName("Should update existing role assignment")
    void testAssignRoleToUser_UpdateExisting() {
        UserRole existing = new UserRole();
        existing.setId("ur-1");
        existing.setUserId("user-1");
        existing.setRoleId("role-1");
        existing.setActive(true);

        when(userRoleRepository.findByUserId("user-1")).thenReturn(Optional.of(existing));
        when(userRoleRepository.save(any(UserRole.class))).thenReturn(existing);

        UserRole result = userRoleService.assignRoleToUser("user-1", "role-2");

        assertThat(result).isNotNull();
        assertThat(result.getRoleId()).isEqualTo("role-2");
        verify(userRoleRepository).save(existing);
    }

    @Test
    @DisplayName("Should get role ID by user ID successfully")
    void testGetRoleIdByUserId_Success() {
        when(userRoleRepository.findByUserIdAndActiveTrue("user-1")).thenReturn(Optional.of(testUserRole));

        Optional<String> result = userRoleService.getRoleIdByUserId("user-1");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("role-1");
    }

    @Test
    @DisplayName("Should return empty when user has no role")
    void testGetRoleIdByUserId_NoRole() {
        when(userRoleRepository.findByUserIdAndActiveTrue("user-1")).thenReturn(Optional.empty());

        Optional<String> result = userRoleService.getRoleIdByUserId("user-1");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should get user role by user ID successfully")
    void testGetUserRoleByUserId_Success() {
        when(userRoleRepository.findByUserIdAndActiveTrue("user-1")).thenReturn(Optional.of(testUserRole));

        Optional<UserRole> result = userRoleService.getUserRoleByUserId("user-1");

        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo("user-1");
    }

    @Test
    @DisplayName("Should check if user has role assignment")
    void testHasRoleAssignment_True() {
        when(userRoleRepository.existsByUserId("user-1")).thenReturn(true);

        boolean result = userRoleService.hasRoleAssignment("user-1");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should check if user has role assignment - false")
    void testHasRoleAssignment_False() {
        when(userRoleRepository.existsByUserId("user-1")).thenReturn(false);

        boolean result = userRoleService.hasRoleAssignment("user-1");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should deactivate user role successfully")
    void testDeactivateUserRole_Success() {
        when(userRoleRepository.findByUserId("user-1")).thenReturn(Optional.of(testUserRole));
        when(userRoleRepository.save(any(UserRole.class))).thenReturn(testUserRole);

        userRoleService.deactivateUserRole("user-1");

        verify(userRoleRepository).save(any(UserRole.class));
        assertThat(testUserRole.isActive()).isFalse();
    }

    @Test
    @DisplayName("Should handle deactivation when user role not found")
    void testDeactivateUserRole_NotFound() {
        when(userRoleRepository.findByUserId("user-1")).thenReturn(Optional.empty());

        userRoleService.deactivateUserRole("user-1");

        verify(userRoleRepository, never()).save(any(UserRole.class));
    }

    @Test
    @DisplayName("Should get all user IDs by role ID successfully")
    void testGetAllUserIdsByRoleId() {
        when(userRoleRepository.findByRoleIdAndActiveTrue("role-1"))
                .thenReturn(Collections.singletonList(testUserRole));

        List<String> result = userRoleService.getAllUserIdsByRoleId("role-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo("user-1");
    }
}

