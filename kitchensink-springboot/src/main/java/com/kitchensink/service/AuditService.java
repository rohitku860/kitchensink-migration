package com.kitchensink.service;

import com.kitchensink.model.AuditLog;
import com.kitchensink.model.Member;
import com.kitchensink.model.User;
import com.kitchensink.repository.AuditLogRepository;
import com.kitchensink.util.CorrelationIdUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuditService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogRepository auditLogRepository;
    
    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }
    
    @Async("auditTaskExecutor")
    public void logMemberCreated(Member member) {
        try {
            String email = member.getEmailEncrypted() != null ? "[ENCRYPTED]" : member.getEmail();
            AuditLog auditLog = new AuditLog(
                "Member",
                member.getId(),
                "CREATE",
                String.format("Member created: %s (%s)", member.getName(), email)
            );
            auditLog.setTimestamp(LocalDateTime.now());
            populateAuditMetadata(auditLog);
            auditLogRepository.save(auditLog);
            logger.debug("Audit log created for member creation: {}", member.getId());
        } catch (Exception e) {
            logger.error("Failed to create audit log for member creation: {}", member.getId(), e);
        }
    }
    
    @Async("auditTaskExecutor")
    public void logMemberUpdated(Member oldMember, Member newMember) {
        try {
            Map<String, String> changedFields = new HashMap<>();
            Map<String, String> oldValues = new HashMap<>();
            Map<String, String> newValues = new HashMap<>();
            StringBuilder details = new StringBuilder("Member updated at " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + ": ");
            
            if (!oldMember.getName().equals(newMember.getName())) {
                changedFields.put("name", "Name");
                oldValues.put("name", oldMember.getName());
                newValues.put("name", newMember.getName());
                details.append(String.format("name: '%s' -> '%s', ", oldMember.getName(), newMember.getName()));
            }
            String oldEmail = oldMember.getEmailEncrypted() != null ? "[ENCRYPTED]" : oldMember.getEmail();
            String newEmail = newMember.getEmailEncrypted() != null ? "[ENCRYPTED]" : newMember.getEmail();
            if (!oldEmail.equals(newEmail)) {
                changedFields.put("email", "Email");
                oldValues.put("email", oldEmail);
                newValues.put("email", newEmail);
                details.append(String.format("email: '%s' -> '%s', ", oldEmail, newEmail));
            }
            String oldPhone = oldMember.getPhoneNumberEncrypted() != null ? "[ENCRYPTED]" : oldMember.getPhoneNumber();
            String newPhone = newMember.getPhoneNumberEncrypted() != null ? "[ENCRYPTED]" : newMember.getPhoneNumber();
            if (!oldPhone.equals(newPhone)) {
                changedFields.put("phoneNumber", "Phone Number");
                oldValues.put("phoneNumber", oldPhone);
                newValues.put("phoneNumber", newPhone);
                details.append(String.format("phone: '%s' -> '%s'", oldPhone, newPhone));
            }
            
            AuditLog auditLog = new AuditLog(
                "Member",
                newMember.getId(),
                "UPDATE",
                details.toString()
            );
            auditLog.setChangedFields(changedFields);
            auditLog.setOldValues(oldValues);
            auditLog.setNewValues(newValues);
            auditLog.setTimestamp(LocalDateTime.now());
            populateAuditMetadata(auditLog);
            auditLogRepository.save(auditLog);
            logger.debug("Audit log created for member update: {}", newMember.getId());
        } catch (Exception e) {
            logger.error("Failed to create audit log for member update: {}", newMember.getId(), e);
        }
    }
    
    @Async("auditTaskExecutor")
    public void logMemberDeleted(Member member) {
        try {
            String email = member.getEmailEncrypted() != null ? "[ENCRYPTED]" : member.getEmail();
            AuditLog auditLog = new AuditLog(
                "Member",
                member.getId(),
                "DELETE",
                String.format("Member deleted: %s (%s)", member.getName(), email)
            );
            auditLog.setTimestamp(LocalDateTime.now());
            populateAuditMetadata(auditLog);
            auditLogRepository.save(auditLog);
            logger.debug("Audit log created for member deletion: {}", member.getId());
        } catch (Exception e) {
            logger.error("Failed to create audit log for member deletion: {}", member.getId(), e);
        }
    }
    
    /**
     * Populates common audit metadata fields from request context.
     * Extracts IP address, correlation ID, and performedBy information.
     */
    private void populateAuditMetadata(AuditLog auditLog) {
        try {
            // Get correlation ID from MDC (set by CorrelationIdFilter)
            String correlationId = CorrelationIdUtil.getCorrelationId();
            if (correlationId != null && !correlationId.isEmpty()) {
                auditLog.setCorrelationId(correlationId);
            }
            
            // Get request attributes (may be null in async context)
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                
                // Extract IP address
                String ipAddress = getClientIpAddress(request);
                if (ipAddress != null && !ipAddress.isEmpty()) {
                    auditLog.setIpAddress(ipAddress);
                }
                
                // Extract performedBy (could be from authentication, API key, or default to SYSTEM)
                String performedBy = extractPerformedBy(request);
                if (performedBy != null && !performedBy.isEmpty()) {
                    auditLog.setPerformedBy(performedBy);
                } else {
                    auditLog.setPerformedBy("SYSTEM");
                }
            } else {
                // Fallback if request context is not available (e.g., in async thread)
                auditLog.setPerformedBy("SYSTEM");
                logger.debug("Request context not available for audit log, using default values");
            }
        } catch (Exception e) {
            logger.warn("Failed to populate audit metadata, using defaults: {}", e.getMessage());
            // Set defaults if extraction fails
            if (auditLog.getPerformedBy() == null) {
                auditLog.setPerformedBy("SYSTEM");
            }
        }
    }
    
    /**
     * Extracts client IP address from request, handling proxy headers.
     */
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
    
    /**
     * Extracts who performed the action (user, API key identifier, etc.).
     * Can be extended to extract from authentication context.
     */
    private String extractPerformedBy(HttpServletRequest request) {
        // Check for authenticated user from Spring Security context
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                return auth.getName(); // Returns userId
            }
        } catch (Exception e) {
            logger.debug("Could not extract user from SecurityContext: {}", e.getMessage());
        }
        
        // Check for API key identifier (if stored in request attribute)
        String apiKeyId = (String) request.getAttribute("apiKeyId");
        if (apiKeyId != null && !apiKeyId.isEmpty()) {
            return "API_KEY:" + apiKeyId;
        }
        
        // Default: return null to use SYSTEM
        return null;
    }
    
    /**
     * Log user creation
     */
    @Async("auditTaskExecutor")
    public void logUserCreated(User user) {
        try {
            AuditLog auditLog = new AuditLog(
                "User",
                user.getId(),
                "CREATE",
                String.format("User created at %s: %s (Email: [ENCRYPTED])", 
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), 
                    user.getName())
            );
            auditLog.setTimestamp(LocalDateTime.now());
            populateAuditMetadata(auditLog);
            auditLogRepository.save(auditLog);
            logger.debug("Audit log created for user creation: {}", user.getId());
        } catch (Exception e) {
            logger.error("Failed to create audit log for user creation: {}", user.getId(), e);
        }
    }
    
    /**
     * Log user update with detailed change tracking
     */
    @Async("auditTaskExecutor")
    public void logUserUpdated(User oldUser, User newUser) {
        try {
            Map<String, String> changedFields = new HashMap<>();
            Map<String, String> oldValues = new HashMap<>();
            Map<String, String> newValues = new HashMap<>();
            StringBuilder details = new StringBuilder(String.format("User updated at %s: ", 
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
            
            // Track name changes
            if (oldUser.getName() != null && newUser.getName() != null && 
                !oldUser.getName().equals(newUser.getName())) {
                changedFields.put("name", "Name");
                oldValues.put("name", oldUser.getName());
                newValues.put("name", newUser.getName());
                details.append(String.format("name: '%s' -> '%s', ", oldUser.getName(), newUser.getName()));
            }
            
            // Track email changes (compare hashes since emails are encrypted)
            if (oldUser.getEmailHash() != null && newUser.getEmailHash() != null && 
                !oldUser.getEmailHash().equals(newUser.getEmailHash())) {
                changedFields.put("email", "Email");
                oldValues.put("email", "[ENCRYPTED]");
                newValues.put("email", "[ENCRYPTED]");
                details.append("email: [CHANGED], ");
            }
            
            // Track phone changes (compare hashes since phones are encrypted)
            if (oldUser.getPhoneNumberHash() != null && newUser.getPhoneNumberHash() != null && 
                !oldUser.getPhoneNumberHash().equals(newUser.getPhoneNumberHash())) {
                changedFields.put("phoneNumber", "Phone Number");
                oldValues.put("phoneNumber", "[ENCRYPTED]");
                newValues.put("phoneNumber", "[ENCRYPTED]");
                details.append("phoneNumber: [CHANGED], ");
            }
            
            // Track other field changes
            if (oldUser.getIsdCode() != null && newUser.getIsdCode() != null && 
                !oldUser.getIsdCode().equals(newUser.getIsdCode())) {
                changedFields.put("isdCode", "ISD Code");
                oldValues.put("isdCode", oldUser.getIsdCode());
                newValues.put("isdCode", newUser.getIsdCode());
                details.append(String.format("isdCode: '%s' -> '%s', ", oldUser.getIsdCode(), newUser.getIsdCode()));
            }
            
            if (oldUser.getDateOfBirth() != null && newUser.getDateOfBirth() != null && 
                !oldUser.getDateOfBirth().equals(newUser.getDateOfBirth())) {
                changedFields.put("dateOfBirth", "Date of Birth");
                oldValues.put("dateOfBirth", oldUser.getDateOfBirth());
                newValues.put("dateOfBirth", newUser.getDateOfBirth());
                details.append(String.format("dateOfBirth: '%s' -> '%s', ", oldUser.getDateOfBirth(), newUser.getDateOfBirth()));
            }
            
            if (oldUser.getAddress() != null && newUser.getAddress() != null && 
                !oldUser.getAddress().equals(newUser.getAddress())) {
                changedFields.put("address", "Address");
                oldValues.put("address", oldUser.getAddress());
                newValues.put("address", newUser.getAddress());
                details.append(String.format("address: '%s' -> '%s', ", oldUser.getAddress(), newUser.getAddress()));
            }
            
            if (oldUser.getCity() != null && newUser.getCity() != null && 
                !oldUser.getCity().equals(newUser.getCity())) {
                changedFields.put("city", "City");
                oldValues.put("city", oldUser.getCity());
                newValues.put("city", newUser.getCity());
                details.append(String.format("city: '%s' -> '%s', ", oldUser.getCity(), newUser.getCity()));
            }
            
            if (oldUser.getCountry() != null && newUser.getCountry() != null && 
                !oldUser.getCountry().equals(newUser.getCountry())) {
                changedFields.put("country", "Country");
                oldValues.put("country", oldUser.getCountry());
                newValues.put("country", newUser.getCountry());
                details.append(String.format("country: '%s' -> '%s', ", oldUser.getCountry(), newUser.getCountry()));
            }
            
            if (oldUser.getStatus() != null && newUser.getStatus() != null && 
                !oldUser.getStatus().equals(newUser.getStatus())) {
                changedFields.put("status", "Status");
                oldValues.put("status", oldUser.getStatus());
                newValues.put("status", newUser.getStatus());
                details.append(String.format("status: '%s' -> '%s', ", oldUser.getStatus(), newUser.getStatus()));
            }
            
            // Remove trailing comma and space
            if (details.length() > 0 && details.toString().endsWith(", ")) {
                details.setLength(details.length() - 2);
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
        } catch (Exception e) {
            logger.error("Failed to create audit log for user update: {}", newUser.getId(), e);
        }
    }
    
    /**
     * Log user deletion
     */
    @Async("auditTaskExecutor")
    public void logUserDeleted(User user) {
        try {
            AuditLog auditLog = new AuditLog(
                "User",
                user.getId(),
                "DELETE",
                String.format("User deleted at %s: %s (Email: [ENCRYPTED])", 
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), 
                    user.getName())
            );
            auditLog.setTimestamp(LocalDateTime.now());
            populateAuditMetadata(auditLog);
            auditLogRepository.save(auditLog);
            logger.debug("Audit log created for user deletion: {}", user.getId());
        } catch (Exception e) {
            logger.error("Failed to create audit log for user deletion: {}", user.getId(), e);
        }
    }
    
    /**
     * Log update request approval
     */
    @Async("auditTaskExecutor")
    public void logUpdateRequestApproved(String requestId, String userId, String fieldName, String adminId) {
        try {
            AuditLog auditLog = new AuditLog(
                "UpdateRequest",
                requestId,
                "APPROVE",
                String.format("Update request approved at %s: User %s, Field '%s' approved by Admin %s", 
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    userId, fieldName, adminId)
            );
            auditLog.setTimestamp(LocalDateTime.now());
            populateAuditMetadata(auditLog);
            auditLogRepository.save(auditLog);
            logger.debug("Audit log created for update request approval: {}", requestId);
        } catch (Exception e) {
            logger.error("Failed to create audit log for update request approval: {}", requestId, e);
        }
    }
    
    /**
     * Log update request rejection
     */
    @Async("auditTaskExecutor")
    public void logUpdateRequestRejected(String requestId, String userId, String fieldName, String adminId, String reason) {
        try {
            AuditLog auditLog = new AuditLog(
                "UpdateRequest",
                requestId,
                "REJECT",
                String.format("Update request rejected at %s: User %s, Field '%s' rejected by Admin %s. Reason: %s", 
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    userId, fieldName, adminId, reason)
            );
            auditLog.setTimestamp(LocalDateTime.now());
            populateAuditMetadata(auditLog);
            auditLogRepository.save(auditLog);
            logger.debug("Audit log created for update request rejection: {}", requestId);
        } catch (Exception e) {
            logger.error("Failed to create audit log for update request rejection: {}", requestId, e);
        }
    }
    
    /**
     * Log update request revocation
     */
    @Async("auditTaskExecutor")
    public void logUpdateRequestRevoked(String requestId, String userId, String fieldName) {
        try {
            AuditLog auditLog = new AuditLog(
                "UpdateRequest",
                requestId,
                "REVOKE",
                String.format("Update request revoked at %s: User %s revoked their own request for Field '%s'", 
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    userId, fieldName)
            );
            auditLog.setTimestamp(LocalDateTime.now());
            populateAuditMetadata(auditLog);
            auditLogRepository.save(auditLog);
            logger.debug("Audit log created for update request revocation: {}", requestId);
        } catch (Exception e) {
            logger.error("Failed to create audit log for update request revocation: {}", requestId, e);
        }
    }
}

