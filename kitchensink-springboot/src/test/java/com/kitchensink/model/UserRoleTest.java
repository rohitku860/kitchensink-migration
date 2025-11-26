package com.kitchensink.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserRole Model Tests")
class UserRoleTest {

    private UserRole userRole;

    @BeforeEach
    void setUp() {
        userRole = new UserRole();
    }

    @Test
    @DisplayName("Should create user role with default values")
    void testUserRoleCreation() {
        assertThat(userRole.isActive()).isTrue();
        assertThat(userRole.getAssignedAt()).isNotNull();
        assertThat(userRole.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should create user role with constructor")
    void testUserRoleConstructor() {
        UserRole newUserRole = new UserRole("user-1", "role-1");

        assertThat(newUserRole.getUserId()).isEqualTo("user-1");
        assertThat(newUserRole.getRoleId()).isEqualTo("role-1");
        assertThat(newUserRole.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should set and get all fields")
    void testGettersAndSetters() {
        userRole.setId("ur-1");
        userRole.setUserId("user-1");
        userRole.setRoleId("role-1");
        LocalDateTime now = LocalDateTime.now();
        userRole.setAssignedAt(now);
        userRole.setUpdatedAt(now);
        userRole.setActive(false);

        assertThat(userRole.getId()).isEqualTo("ur-1");
        assertThat(userRole.getUserId()).isEqualTo("user-1");
        assertThat(userRole.getRoleId()).isEqualTo("role-1");
        assertThat(userRole.getAssignedAt()).isEqualTo(now);
        assertThat(userRole.getUpdatedAt()).isEqualTo(now);
        assertThat(userRole.isActive()).isFalse();
    }
}

