package com.kitchensink.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "audit_logs")
public class AuditLog {
    
    @Id
    private String id;
    
    private String entityType;
    private String entityId;
    private String action;
    private String performedBy;
    private LocalDateTime timestamp;
    private String details;
    private String ipAddress;
    private String correlationId;
    
    // Enhanced fields for better clarity
    private Map<String, String> changedFields; // Map of field names that changed
    private Map<String, String> oldValues; // Map of old values (field -> old value)
    private Map<String, String> newValues; // Map of new values (field -> new value)
    
    public AuditLog() {
        this.timestamp = LocalDateTime.now();
    }
    
    public AuditLog(String entityType, String entityId, String action, String details) {
        this();
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.details = details;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getEntityType() {
        return entityType;
    }
    
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }
    
    public String getEntityId() {
        return entityId;
    }
    
    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public String getPerformedBy() {
        return performedBy;
    }
    
    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getDetails() {
        return details;
    }
    
    public void setDetails(String details) {
        this.details = details;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    
    public Map<String, String> getChangedFields() {
        return changedFields;
    }
    
    public void setChangedFields(Map<String, String> changedFields) {
        this.changedFields = changedFields;
    }
    
    public Map<String, String> getOldValues() {
        return oldValues;
    }
    
    public void setOldValues(Map<String, String> oldValues) {
        this.oldValues = oldValues;
    }
    
    public Map<String, String> getNewValues() {
        return newValues;
    }
    
    public void setNewValues(Map<String, String> newValues) {
        this.newValues = newValues;
    }
}

