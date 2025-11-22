package com.kitchensink.service;

import com.kitchensink.model.AuditLog;
import com.kitchensink.model.Member;
import com.kitchensink.repository.AuditLogRepository;
import com.kitchensink.util.CorrelationIdUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

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
            StringBuilder details = new StringBuilder("Member updated: ");
            if (!oldMember.getName().equals(newMember.getName())) {
                details.append(String.format("name: %s -> %s, ", oldMember.getName(), newMember.getName()));
            }
            String oldEmail = oldMember.getEmailEncrypted() != null ? "[ENCRYPTED]" : oldMember.getEmail();
            String newEmail = newMember.getEmailEncrypted() != null ? "[ENCRYPTED]" : newMember.getEmail();
            if (!oldEmail.equals(newEmail)) {
                details.append(String.format("email: %s -> %s, ", oldEmail, newEmail));
            }
            String oldPhone = oldMember.getPhoneNumberEncrypted() != null ? "[ENCRYPTED]" : oldMember.getPhoneNumber();
            String newPhone = newMember.getPhoneNumberEncrypted() != null ? "[ENCRYPTED]" : newMember.getPhoneNumber();
            if (!oldPhone.equals(newPhone)) {
                details.append(String.format("phone: %s -> %s", oldPhone, newPhone));
            }
            
            AuditLog auditLog = new AuditLog(
                "Member",
                newMember.getId(),
                "UPDATE",
                details.toString()
            );
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
        // Check for API key identifier (if stored in request attribute)
        String apiKeyId = (String) request.getAttribute("apiKeyId");
        if (apiKeyId != null && !apiKeyId.isEmpty()) {
            return "API_KEY:" + apiKeyId;
        }
        
        // Check for authenticated user (if Spring Security is used)
        // Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // if (auth != null && auth.isAuthenticated()) {
        //     return auth.getName();
        // }
        
        // Default: return null to use SYSTEM
        return null;
    }
}

