package com.kitchensink.listener;

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
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;

import jakarta.servlet.http.HttpServletRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserMongoEventListener Tests")
class UserMongoEventListenerTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private UserMongoEventListener userMongoEventListener;

    private User testUser;
    private UserSnapshot oldUser;

    @BeforeEach
    void setUp() {
        CorrelationIdUtil.clear();
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();

        testUser = new User();
        testUser.setId("user-1");
        testUser.setName("Test User");
        testUser.setEmailHash("email-hash");
        testUser.setPhoneNumberHash("phone-hash");
        testUser.setIsdCode("+91");
        testUser.setDateOfBirth("01-01-1990");
        testUser.setAddress("Address");
        testUser.setCity("City");
        testUser.setCountry("Country");
        testUser.setStatus("ACTIVE");

        User oldUserEntity = new User();
        oldUserEntity.setId("user-1");
        oldUserEntity.setName("Old Name");
        oldUserEntity.setEmailHash("old-email-hash");
        oldUserEntity.setPhoneNumberHash("old-phone-hash");
        oldUserEntity.setIsdCode("+1");
        oldUserEntity.setDateOfBirth("01-01-1980");
        oldUserEntity.setAddress("Old Address");
        oldUserEntity.setCity("Old City");
        oldUserEntity.setCountry("Old Country");
        oldUserEntity.setStatus("INACTIVE");
        
        oldUser = UserSnapshot.from(oldUserEntity);
    }

    @AfterEach
    void tearDown() {
        CorrelationIdUtil.clear();
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
        UserMongoEventListener.setOldUserState(null);
    }

    @Test
    @DisplayName("Should handle before convert event")
    void testOnBeforeConvert() {
        BeforeConvertEvent<User> event = new BeforeConvertEvent<>(testUser, "users");

        userMongoEventListener.onBeforeConvert(event);

        // Should not throw exception
    }

    @Test
    @DisplayName("Should create audit log for new user")
    void testOnAfterSave_NewUser() {
        testUser.setId(null); // New user
        org.bson.Document document = new org.bson.Document();
        AfterSaveEvent<User> event = new AfterSaveEvent<>(testUser, document, "users");
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        userMongoEventListener.onAfterSave(event);

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should create audit log for user update")
    void testOnAfterSave_UpdateUser() {
        UserMongoEventListener.setOldUserState(oldUser);
        org.bson.Document document = new org.bson.Document();
        AfterSaveEvent<User> event = new AfterSaveEvent<>(testUser, document, "users");
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        userMongoEventListener.onAfterSave(event);

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should handle after delete event")
    void testOnAfterDelete() {
        org.bson.Document document = new org.bson.Document();
        AfterDeleteEvent<User> event = new AfterDeleteEvent<>(document, User.class, "users");

        userMongoEventListener.onAfterDelete(event);

        // Should not throw exception
    }

    @Test
    @DisplayName("Should set old user state")
    void testSetOldUserState() {
        UserMongoEventListener.setOldUserState(oldUser);

        // State should be set in ThreadLocal
        // This is tested indirectly through onAfterSave
    }

    @Test
    @DisplayName("Should handle exception during audit log creation")
    void testOnAfterSave_Exception() {
        org.bson.Document document = new org.bson.Document();
        AfterSaveEvent<User> event = new AfterSaveEvent<>(testUser, document, "users");
        when(auditLogRepository.save(any(AuditLog.class))).thenThrow(new RuntimeException("DB Error"));

        userMongoEventListener.onAfterSave(event);

        // Should not throw exception, just log error
        verify(auditLogRepository).save(any(AuditLog.class));
    }
}

