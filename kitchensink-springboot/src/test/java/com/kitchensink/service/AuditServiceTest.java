package com.kitchensink.service;

import com.kitchensink.model.AuditLog;
import com.kitchensink.model.User;
import com.kitchensink.repository.AuditLogRepository;
import com.kitchensink.util.CorrelationIdUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditService Tests")
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AuditService auditService;

    private User testUser;

    @BeforeEach
    void setUp() {
        CorrelationIdUtil.clear();
        testUser = new User();
        testUser.setId("user-1");
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setEmailHash("email-hash");
        testUser.setPhoneNumberHash("phone-hash");
        testUser.setIsdCode("+91");
        testUser.setDateOfBirth("01-01-1990");
        testUser.setAddress("Address");
        testUser.setCity("City");
        testUser.setCountry("Country");
        testUser.setStatus("ACTIVE");
    }

    @AfterEach
    void tearDown() {
        CorrelationIdUtil.clear();
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Should log user creation successfully")
    void testLogUserCreated() {
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        auditService.logUserCreated(testUser);

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should log user update successfully")
    void testLogUserUpdated() {
        User oldUser = new User();
        oldUser.setId("user-1");
        oldUser.setName("Old Name");
        oldUser.setEmailHash("old-email-hash");
        oldUser.setPhoneNumberHash("old-phone-hash");
        oldUser.setIsdCode("+1");
        oldUser.setDateOfBirth("01-01-1980");
        oldUser.setAddress("Old Address");
        oldUser.setCity("Old City");
        oldUser.setCountry("Old Country");
        oldUser.setStatus("INACTIVE");

        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        auditService.logUserUpdated(oldUser, testUser);

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should log user deletion successfully")
    void testLogUserDeleted() {
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        auditService.logUserDeleted(testUser);

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should log update request approval")
    void testLogUpdateRequestApproved() {
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        auditService.logUpdateRequestApproved("req-1", "user-1", "name", "admin-1");

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should log update request rejection")
    void testLogUpdateRequestRejected() {
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        auditService.logUpdateRequestRejected("req-1", "user-1", "name", "admin-1", "Invalid");

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should log update request revocation")
    void testLogUpdateRequestRevoked() {
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        auditService.logUpdateRequestRevoked("req-1", "user-1", "name");

        verify(auditLogRepository).save(any(AuditLog.class));
    }
}

