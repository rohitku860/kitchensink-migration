package com.kitchensink.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User Model Tests")
class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
    }

    @Test
    @DisplayName("Should create user with default values")
    void testUserCreation() {
        assertThat(user.getStatus()).isEqualTo("ACTIVE");
        assertThat(user.getRegistrationDate()).isNotNull();
    }

    @Test
    @DisplayName("Should set and get all fields")
    void testGettersAndSetters() {
        user.setId("user-1");
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPhoneNumber("9876543210");
        user.setIsdCode("+91");
        user.setDateOfBirth("01-01-1990");
        user.setAddress("Address");
        user.setCity("City");
        user.setCountry("Country");
        user.setStatus("ACTIVE");
        user.setEmailHash("email-hash");
        user.setPhoneNumberHash("phone-hash");
        user.setEmailEncrypted("encrypted-email");
        user.setPhoneNumberEncrypted("encrypted-phone");
        LocalDateTime now = LocalDateTime.now();
        user.setRegistrationDate(now);
        user.setLastLoginDate(now);

        assertThat(user.getId()).isEqualTo("user-1");
        assertThat(user.getName()).isEqualTo("Test User");
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getPhoneNumber()).isEqualTo("9876543210");
        assertThat(user.getIsdCode()).isEqualTo("+91");
        assertThat(user.getDateOfBirth()).isEqualTo("01-01-1990");
        assertThat(user.getAddress()).isEqualTo("Address");
        assertThat(user.getCity()).isEqualTo("City");
        assertThat(user.getCountry()).isEqualTo("Country");
        assertThat(user.getStatus()).isEqualTo("ACTIVE");
        assertThat(user.getEmailHash()).isEqualTo("email-hash");
        assertThat(user.getPhoneNumberHash()).isEqualTo("phone-hash");
        assertThat(user.getEmailEncrypted()).isEqualTo("encrypted-email");
        assertThat(user.getPhoneNumberEncrypted()).isEqualTo("encrypted-phone");
        assertThat(user.getRegistrationDate()).isEqualTo(now);
        assertThat(user.getLastLoginDate()).isEqualTo(now);
    }
}

