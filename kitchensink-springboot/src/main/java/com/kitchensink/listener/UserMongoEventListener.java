package com.kitchensink.listener;

import com.kitchensink.model.AuditLog;
import com.kitchensink.model.User;
import com.kitchensink.repository.AuditLogRepository;
import com.kitchensink.util.CorrelationIdUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
public class UserMongoEventListener extends AbstractMongoEventListener<User> {
    
    private static final Logger logger = LoggerFactory.getLogger(UserMongoEventListener.class);
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    private static final ThreadLocal<UserSnapshot> oldUserState = new ThreadLocal<>();
    
    @Override
    public void onBeforeConvert(BeforeConvertEvent<User> event) {
        User user = event.getSource();
        if (user.getId() != null) {
            try {
                // Old state is set by service layer before save
            } catch (Exception e) {
                logger.debug("Could not retrieve old user state: {}", e.getMessage());
            }
        }
    }
    
    @Override
    public void onAfterSave(AfterSaveEvent<User> event) {
        User user = event.getSource();
        UserSnapshot oldUser = oldUserState.get();
        
        try {
            if (oldUser == null) {
                createAuditLog(user, "CREATE", null);
            } else {
                createUpdateAuditLog(oldUser, user);
            }
        } catch (Exception e) {
            logger.error("Failed to create audit log for user: {}", user.getId(), e);
        } finally {
            oldUserState.remove();
        }
    }
    
    @Override
    public void onAfterDelete(AfterDeleteEvent<User> event) {
        // Note: AfterDeleteEvent doesn't have the source object, only the document
        // For deletion, we need to log before deletion, so it's handled in service layer
        // This is a limitation of MongoDB event listeners for delete operations
    }
    
    public static void setOldUserState(UserSnapshot oldUser) {
        oldUserState.set(oldUser);
    }
    
    private void createAuditLog(User user, String action, String details) {
        if (details == null) {
            details = String.format("User %s at %s: %s (Email: [ENCRYPTED])", 
                action.toLowerCase() + "d",
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), 
                user.getName());
        }
        
        AuditLog auditLog = new AuditLog(
            "User",
            user.getId(),
            action,
            details
        );
        auditLog.setTimestamp(LocalDateTime.now());
        populateAuditMetadata(auditLog);
        auditLogRepository.save(auditLog);
        logger.debug("Audit log created for user {}: {}", action, user.getId());
    }
    
    private void createUpdateAuditLog(UserSnapshot oldUser, User newUser) {
        Map<String, String> changedFields = new HashMap<>();
        Map<String, String> oldValues = new HashMap<>();
        Map<String, String> newValues = new HashMap<>();
        StringBuilder details = new StringBuilder(String.format("User updated at %s: ", 
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        
        if (oldUser.getName() != null && newUser.getName() != null && 
            !oldUser.getName().equals(newUser.getName())) {
            changedFields.put("name", "Name");
            oldValues.put("name", oldUser.getName());
            newValues.put("name", newUser.getName());
            details.append(String.format("name: '%s' -> '%s', ", oldUser.getName(), newUser.getName()));
        }
        
        if (oldUser.getEmailHash() != null && newUser.getEmailHash() != null && 
            !oldUser.getEmailHash().equals(newUser.getEmailHash())) {
            changedFields.put("email", "Email");
            oldValues.put("email", "[ENCRYPTED]");
            newValues.put("email", "[ENCRYPTED]");
            details.append("email: [CHANGED], ");
        }
        
        if (oldUser.getPhoneNumberHash() != null && newUser.getPhoneNumberHash() != null && 
            !oldUser.getPhoneNumberHash().equals(newUser.getPhoneNumberHash())) {
            changedFields.put("phoneNumber", "Phone Number");
            oldValues.put("phoneNumber", "[ENCRYPTED]");
            newValues.put("phoneNumber", "[ENCRYPTED]");
            details.append("phoneNumber: [CHANGED], ");
        }
        
        trackFieldChange(oldUser.getIsdCode(), newUser.getIsdCode(), "isdCode", "ISD Code", changedFields, oldValues, newValues, details);
        trackFieldChange(oldUser.getDateOfBirth(), newUser.getDateOfBirth(), "dateOfBirth", "Date of Birth", changedFields, oldValues, newValues, details);
        trackFieldChange(oldUser.getAddress(), newUser.getAddress(), "address", "Address", changedFields, oldValues, newValues, details);
        trackFieldChange(oldUser.getCity(), newUser.getCity(), "city", "City", changedFields, oldValues, newValues, details);
        trackFieldChange(oldUser.getCountry(), newUser.getCountry(), "country", "Country", changedFields, oldValues, newValues, details);
        trackFieldChange(oldUser.getStatus(), newUser.getStatus(), "status", "Status", changedFields, oldValues, newValues, details);
        
        // Remove trailing comma and space
        if (details.length() > 0 && details.toString().endsWith(", ")) {
            details.setLength(details.length() - 2);
        }
        
        if (changedFields.isEmpty()) {
            logger.debug("No changes detected, skipping audit log");
            return;
        }
        
        AuditLog auditLog = new AuditLog(
            "User",
            newUser.getId(),
            "UPDATE",
            details.toString()
        );
        auditLog.setChangedFields(changedFields);
        auditLog.setOldValues(oldValues);
        auditLog.setNewValues(newValues);
        auditLog.setTimestamp(LocalDateTime.now());
        populateAuditMetadata(auditLog);
        auditLogRepository.save(auditLog);
        logger.debug("Audit log created for user update: {}", newUser.getId());
    }
    
    private void trackFieldChange(String oldValue, String newValue, String fieldKey, String fieldLabel,
                                  Map<String, String> changedFields, Map<String, String> oldValues, 
                                  Map<String, String> newValues, StringBuilder details) {
        if (oldValue != null && newValue != null && !oldValue.equals(newValue)) {
            changedFields.put(fieldKey, fieldLabel);
            oldValues.put(fieldKey, oldValue);
            newValues.put(fieldKey, newValue);
            details.append(String.format("%s: '%s' -> '%s', ", fieldKey, oldValue, newValue));
        }
    }
    
    private void populateAuditMetadata(AuditLog auditLog) {
        try {
            String correlationId = CorrelationIdUtil.getCorrelationId();
            if (correlationId != null && !correlationId.isEmpty()) {
                auditLog.setCorrelationId(correlationId);
            }
            
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                
                String ipAddress = getClientIpAddress(request);
                if (ipAddress != null && !ipAddress.isEmpty()) {
                    auditLog.setIpAddress(ipAddress);
                }
                
                String performedBy = extractPerformedBy();
                if (performedBy != null && !performedBy.isEmpty()) {
                    auditLog.setPerformedBy(performedBy);
                } else {
                    auditLog.setPerformedBy("SYSTEM");
                }
            } else {
                auditLog.setPerformedBy("SYSTEM");
            }
        } catch (Exception e) {
            logger.warn("Failed to populate audit metadata: {}", e.getMessage());
            if (auditLog.getPerformedBy() == null) {
                auditLog.setPerformedBy("SYSTEM");
            }
        }
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
    
    private String extractPerformedBy() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                return auth.getName();
            }
        } catch (Exception e) {
            logger.debug("Could not extract user from SecurityContext: {}", e.getMessage());
        }
        return null;
    }
}

