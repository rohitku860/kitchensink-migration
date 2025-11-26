package com.kitchensink.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuditLog Model Tests")
class AuditLogTest {

    private AuditLog auditLog;

    @BeforeEach
    void setUp() {
        auditLog = new AuditLog();
    }

    @Test
    @DisplayName("Should create audit log with default values")
    void testAuditLogCreation() {
        assertThat(auditLog.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should create audit log with constructor")
    void testAuditLogConstructor() {
        AuditLog newLog = new AuditLog("User", "user-1", "CREATE", "User created");

        assertThat(newLog.getEntityType()).isEqualTo("User");
        assertThat(newLog.getEntityId()).isEqualTo("user-1");
        assertThat(newLog.getAction()).isEqualTo("CREATE");
        assertThat(newLog.getDetails()).isEqualTo("User created");
    }

    @Test
    @DisplayName("Should set and get all fields")
    void testGettersAndSetters() {
        auditLog.setId("log-1");
        auditLog.setEntityType("User");
        auditLog.setEntityId("user-1");
        auditLog.setAction("UPDATE");
        auditLog.setPerformedBy("admin-1");
        LocalDateTime now = LocalDateTime.now();
        auditLog.setTimestamp(now);
        auditLog.setDetails("User updated");
        auditLog.setIpAddress("127.0.0.1");
        auditLog.setCorrelationId("corr-1");
        Map<String, String> changedFields = new HashMap<>();
        changedFields.put("name", "Name");
        auditLog.setChangedFields(changedFields);
        Map<String, String> oldValues = new HashMap<>();
        oldValues.put("name", "Old Name");
        auditLog.setOldValues(oldValues);
        Map<String, String> newValues = new HashMap<>();
        newValues.put("name", "New Name");
        auditLog.setNewValues(newValues);

        assertThat(auditLog.getId()).isEqualTo("log-1");
        assertThat(auditLog.getEntityType()).isEqualTo("User");
        assertThat(auditLog.getEntityId()).isEqualTo("user-1");
        assertThat(auditLog.getAction()).isEqualTo("UPDATE");
        assertThat(auditLog.getPerformedBy()).isEqualTo("admin-1");
        assertThat(auditLog.getTimestamp()).isEqualTo(now);
        assertThat(auditLog.getDetails()).isEqualTo("User updated");
        assertThat(auditLog.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(auditLog.getCorrelationId()).isEqualTo("corr-1");
        assertThat(auditLog.getChangedFields()).isEqualTo(changedFields);
        assertThat(auditLog.getOldValues()).isEqualTo(oldValues);
        assertThat(auditLog.getNewValues()).isEqualTo(newValues);
    }
}

