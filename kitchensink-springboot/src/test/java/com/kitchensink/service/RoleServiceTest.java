package com.kitchensink.service;

import com.kitchensink.exception.ResourceNotFoundException;
import com.kitchensink.model.Role;
import com.kitchensink.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleService Tests")
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleService userRoleService;

    @InjectMocks
    private RoleService roleService;

    private Role adminRole;
    private Role userRole;

    @BeforeEach
    void setUp() {
        adminRole = new Role();
        adminRole.setId("role-1");
        adminRole.setName("ADMIN");

        userRole = new Role();
        userRole.setId("role-2");
        userRole.setName("USER");
    }

    @Test
    @DisplayName("Should get role by name successfully")
    void testGetRoleByName_Success() {
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));

        Role result = roleService.getRoleByName("ADMIN");

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Should throw exception when role not found by name")
    void testGetRoleByName_NotFound() {
        when(roleRepository.findByName("INVALID")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.getRoleByName("INVALID"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should get role by ID successfully")
    void testGetRoleById_Success() {
        when(roleRepository.findById("role-1")).thenReturn(Optional.of(adminRole));

        Role result = roleService.getRoleById("role-1");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("role-1");
    }

    @Test
    @DisplayName("Should create role if not exists")
    void testCreateRoleIfNotExists_NewRole() {
        when(roleRepository.findByName("NEW_ROLE")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenReturn(adminRole);

        Role result = roleService.createRoleIfNotExists("NEW_ROLE", "New Role Description");

        assertThat(result).isNotNull();
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    @DisplayName("Should return existing role if already exists")
    void testCreateRoleIfNotExists_ExistingRole() {
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));

        Role result = roleService.createRoleIfNotExists("ADMIN", "Admin Role");

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("ADMIN");
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    @DisplayName("Should check if role is admin")
    void testIsAdmin_True() {
        when(roleRepository.findById("role-1")).thenReturn(Optional.of(adminRole));

        boolean result = roleService.isAdmin("role-1");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should check if role is admin - false")
    void testIsAdmin_False() {
        when(roleRepository.findById("role-2")).thenReturn(Optional.of(userRole));

        boolean result = roleService.isAdmin("role-2");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should check if role is user")
    void testIsUser_True() {
        when(roleRepository.findById("role-2")).thenReturn(Optional.of(userRole));

        boolean result = roleService.isUser("role-2");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should check if user is admin by userId")
    void testIsAdminByUserId_True() {
        when(userRoleService.getRoleIdByUserId("user-1")).thenReturn(Optional.of("role-1"));
        when(roleRepository.findById("role-1")).thenReturn(Optional.of(adminRole));

        boolean result = roleService.isAdminByUserId("user-1");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should check if user is admin by userId - false")
    void testIsAdminByUserId_False() {
        when(userRoleService.getRoleIdByUserId("user-1")).thenReturn(Optional.of("role-2"));
        when(roleRepository.findById("role-2")).thenReturn(Optional.of(userRole));

        boolean result = roleService.isAdminByUserId("user-1");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when user has no role")
    void testIsAdminByUserId_NoRole() {
        when(userRoleService.getRoleIdByUserId("user-1")).thenReturn(Optional.empty());

        boolean result = roleService.isAdminByUserId("user-1");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should get role name by user ID successfully")
    void testGetRoleNameByUserId_Success() {
        when(userRoleService.getRoleIdByUserId("user-1")).thenReturn(Optional.of("role-1"));
        when(roleRepository.findById("role-1")).thenReturn(Optional.of(adminRole));

        String result = roleService.getRoleNameByUserId("user-1");

        assertThat(result).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Should throw exception when role not found for user")
    void testGetRoleNameByUserId_NotFound() {
        when(userRoleService.getRoleIdByUserId("user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.getRoleNameByUserId("user-1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should get role ID by user ID successfully")
    void testGetRoleIdByUserId_Success() {
        when(userRoleService.getRoleIdByUserId("user-1")).thenReturn(Optional.of("role-1"));

        String result = roleService.getRoleIdByUserId("user-1");

        assertThat(result).isEqualTo("role-1");
    }

    @Test
    @DisplayName("Should throw exception when role ID not found for user")
    void testGetRoleIdByUserId_NotFound() {
        when(userRoleService.getRoleIdByUserId("user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.getRoleIdByUserId("user-1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

