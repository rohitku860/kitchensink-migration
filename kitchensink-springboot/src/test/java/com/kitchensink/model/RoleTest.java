package com.kitchensink.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Role Model Tests")
class RoleTest {

    private Role role;

    @BeforeEach
    void setUp() {
        role = new Role();
    }

    @Test
    @DisplayName("Should create role with default values")
    void testRoleCreation() {
        assertThat(role.isActive()).isTrue();
        assertThat(role.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should create role with constructor")
    void testRoleConstructor() {
        Role newRole = new Role("ADMIN", "Admin Role");

        assertThat(newRole.getName()).isEqualTo("ADMIN");
        assertThat(newRole.getDescription()).isEqualTo("Admin Role");
        assertThat(newRole.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should set and get all fields")
    void testGettersAndSetters() {
        role.setId("role-1");
        role.setName("USER");
        role.setDescription("User Role");
        LocalDateTime now = LocalDateTime.now();
        role.setCreatedAt(now);
        role.setActive(false);

        assertThat(role.getId()).isEqualTo("role-1");
        assertThat(role.getName()).isEqualTo("USER");
        assertThat(role.getDescription()).isEqualTo("User Role");
        assertThat(role.getCreatedAt()).isEqualTo(now);
        assertThat(role.isActive()).isFalse();
    }
}

