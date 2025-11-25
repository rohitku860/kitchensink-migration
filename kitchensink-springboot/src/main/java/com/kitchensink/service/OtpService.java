package com.kitchensink.service;

import com.kitchensink.model.Otp;
import com.kitchensink.repository.OtpRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OtpService {
    
    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private final OtpRepository otpRepository;
    private final EncryptionService encryptionService;
    private static final SecureRandom random = new SecureRandom();
    private static final int OTP_LENGTH = 6;
    
    @Value("${app.otp.max-attempts-per-window:1000}")
    private int maxAttemptsPerWindow;
    
    @Value("${app.otp.rate-limit-window-minutes:15}")
    private int rateLimitWindowMinutes;
    
    public OtpService(OtpRepository otpRepository, EncryptionService encryptionService) {
        this.otpRepository = otpRepository;
        this.encryptionService = encryptionService;
    }
    
    /**
     * Generate a 6-digit OTP
     */
    public String generateOtp() {
        int otp = 100000 + random.nextInt(900000); // 6-digit OTP
        return String.valueOf(otp);
    }
    
    /**
     * Check if OTP rate limit is exceeded for the given email and purpose
     */
    private void checkRateLimit(String emailHash, String purpose) {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(rateLimitWindowMinutes);
        long attemptCount = otpRepository.countByEmailHashAndPurposeAndCreatedAtAfter(emailHash, purpose, windowStart);
        
        if (attemptCount >= maxAttemptsPerWindow) {
            logger.warn("OTP rate limit exceeded for email: [REDACTED], purpose: {}. Attempts: {}/{}, Window: {} minutes", 
                    purpose, attemptCount, maxAttemptsPerWindow, rateLimitWindowMinutes);
            throw new com.kitchensink.exception.ResourceConflictException(
                    String.format("Maximum OTP attempts (%d) exceeded within %d minutes. Please try again later.", 
                            maxAttemptsPerWindow, rateLimitWindowMinutes),
                    "otp");
        }
        
        logger.debug("OTP rate limit check passed. Attempts: {}/{}, Window: {} minutes", 
                attemptCount, maxAttemptsPerWindow, rateLimitWindowMinutes);
    }
    
    /**
     * Create and save OTP for email
     */
    public Otp createOtp(String email, String purpose) {
        logger.debug("Creating OTP for email: [REDACTED], purpose: {}", purpose);
        
        String emailHash = encryptionService.hash(email);
        
        // Check rate limit before creating OTP
        checkRateLimit(emailHash, purpose);
        
        // Invalidate any existing unused OTPs for this email and purpose
        List<Otp> existingOtps = otpRepository.findByEmailHashAndPurposeAndUsedFalse(emailHash, purpose);
        for (Otp existing : existingOtps) {
            existing.setUsed(true);
            otpRepository.save(existing);
        }
        
        // Create new OTP
        String otpCode = generateOtp();
        String otpHash = encryptionService.hash(otpCode);
        Otp otp = new Otp(email, otpCode, purpose);
        otp.setEmailHash(emailHash);
        otp.setOtpHash(otpHash);
        
        Otp saved = otpRepository.save(otp);
        logger.info("OTP created successfully for purpose: {}", purpose);
        return saved;
    }
    
    /**
     * Verify OTP using hash comparison
     */
    public boolean verifyOtp(String email, String otpCode, String purpose) {
        logger.debug("Verifying OTP for email: [REDACTED], purpose: {}", purpose);
        
        String emailHash = encryptionService.hash(email);
        String otpHash = encryptionService.hash(otpCode);
        
        // Find OTP by email hash and purpose
        List<Otp> otps = otpRepository.findByEmailHashAndPurposeAndUsedFalse(emailHash, purpose);
        
        if (otps.isEmpty()) {
            logger.warn("OTP not found or already used");
            return false;
        }
        
        // Find matching OTP by hash
        Optional<Otp> otpOpt = otps.stream()
                .filter(otp -> otpHash.equals(otp.getOtpHash()))
                .findFirst();
        
        if (otpOpt.isEmpty()) {
            logger.warn("Invalid OTP code");
            return false;
        }
        
        Otp otp = otpOpt.get();
        
        // Check expiration
        if (otp.isExpired()) {
            logger.warn("OTP expired");
            return false;
        }
        
        // Mark as used
        otp.setUsed(true);
        otpRepository.save(otp);
        logger.info("OTP verified successfully");
        return true;
    }
    
    /**
     * Get OTP by email and code (for email change workflow) - uses hash comparison
     */
    public Optional<Otp> getOtpByEmailAndCode(String email, String otpCode) {
        String emailHash = encryptionService.hash(email);
        String otpHash = encryptionService.hash(otpCode);
        
        List<Otp> otps = otpRepository.findByEmailHashAndPurposeAndUsedFalse(emailHash, "EMAIL_CHANGE");
        return otps.stream()
                .filter(otp -> otpHash.equals(otp.getOtpHash()))
                .findFirst();
    }
    
    /**
     * Clean up expired OTPs (async)
     */
    @Async
    public void cleanupExpiredOtps() {
        logger.debug("Cleaning up expired OTPs");
        LocalDateTime now = LocalDateTime.now();
        otpRepository.deleteByExpiresAtBefore(now);
        logger.info("Expired OTPs cleaned up");
    }
}

