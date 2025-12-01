package com.kitchensink.service;

import com.kitchensink.model.Otp;
import com.kitchensink.model.OtpVerificationAttempt;
import com.kitchensink.repository.OtpRepository;
import com.kitchensink.repository.OtpVerificationAttemptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OtpService {
    
    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private final OtpRepository otpRepository;
    private final OtpVerificationAttemptRepository verificationAttemptRepository;
    private final EncryptionService encryptionService;
    private static final SecureRandom random = new SecureRandom();
    
    @Value("${app.otp.max-attempts-per-window:1000}")
    private int maxAttemptsPerWindow;
    
    @Value("${app.otp.rate-limit-window-minutes:15}")
    private int rateLimitWindowMinutes;
    
    @Value("${app.otp.max-failed-verification-attempts:5}")
    private int maxFailedVerificationAttempts;
    
    @Value("${app.otp.lockout-duration-minutes:15}")
    private int lockoutDurationMinutes;
    
    public OtpService(OtpRepository otpRepository, 
                     OtpVerificationAttemptRepository verificationAttemptRepository,
                     EncryptionService encryptionService) {
        this.otpRepository = otpRepository;
        this.verificationAttemptRepository = verificationAttemptRepository;
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
    
    private OtpVerificationAttempt getOrCreateVerificationAttempt(String emailHash, String purpose) {
        Optional<OtpVerificationAttempt> existing = verificationAttemptRepository.findByEmailHashAndPurpose(emailHash, purpose);
        if (existing.isPresent()) {
            OtpVerificationAttempt attempt = existing.get();
            if (attempt.getLockoutUntil() != null && LocalDateTime.now().isAfter(attempt.getLockoutUntil())) {
                attempt.reset();
            }
            return attempt;
        }
        return new OtpVerificationAttempt(emailHash, purpose);
    }
    
    private void checkLockoutStatus(String emailHash, String purpose) {
        Optional<OtpVerificationAttempt> attemptOpt = verificationAttemptRepository.findByEmailHashAndPurpose(emailHash, purpose);
        
        if (attemptOpt.isPresent()) {
            OtpVerificationAttempt attempt = attemptOpt.get();
            
            if (attempt.isLockedOut()) {
                long minutesRemaining = java.time.Duration.between(LocalDateTime.now(), attempt.getLockoutUntil()).toMinutes();
                logger.warn("OTP verification locked out for email: [REDACTED], purpose: {}. Lockout expires in {} minutes", 
                        purpose, minutesRemaining);
                throw new com.kitchensink.exception.ResourceConflictException(
                        String.format("Too many failed OTP verification attempts. Please try again after %d minutes.", 
                                minutesRemaining + 1),
                        "otp");
            }
            
            if (attempt.getLockoutUntil() != null && LocalDateTime.now().isAfter(attempt.getLockoutUntil())) {
                attempt.reset();
                verificationAttemptRepository.save(attempt);
            }
        }
    }
    
    private void recordFailedAttempt(String emailHash, String purpose) {
        OtpVerificationAttempt attempt = getOrCreateVerificationAttempt(emailHash, purpose);
        
        attempt.incrementFailedAttempts();
        
        if (attempt.getFailedAttempts() >= maxFailedVerificationAttempts) {
            LocalDateTime lockoutUntil = LocalDateTime.now().plusMinutes(lockoutDurationMinutes);
            attempt.setLockoutUntil(lockoutUntil);
            verificationAttemptRepository.save(attempt);
            
            logger.warn("OTP verification locked out for email: [REDACTED], purpose: {}. Failed attempts: {}/{}, Lockout until: {}", 
                    purpose, attempt.getFailedAttempts(), maxFailedVerificationAttempts, lockoutUntil);
            throw new com.kitchensink.exception.ResourceConflictException(
                    String.format("Too many failed OTP verification attempts (%d). Account locked for %d minutes.", 
                            attempt.getFailedAttempts(), lockoutDurationMinutes),
                    "otp");
        }
        
        verificationAttemptRepository.save(attempt);
        logger.debug("Failed OTP verification attempt recorded. Attempts: {}/{}", 
                attempt.getFailedAttempts(), maxFailedVerificationAttempts);
    }
    
    private void resetFailedAttempts(String emailHash, String purpose) {
        Optional<OtpVerificationAttempt> attemptOpt = verificationAttemptRepository.findByEmailHashAndPurpose(emailHash, purpose);
        if (attemptOpt.isPresent()) {
            OtpVerificationAttempt attempt = attemptOpt.get();
            attempt.reset();
            verificationAttemptRepository.save(attempt);
            logger.debug("Failed OTP verification attempts reset for email: [REDACTED], purpose: {}", purpose);
        }
    }
    
    /**
     * Verify OTP using hash comparison
     */
    public boolean verifyOtp(String email, String otpCode, String purpose) {
        logger.debug("Verifying OTP for email: [REDACTED], purpose: {}", purpose);
        
        String emailHash = encryptionService.hash(email);
        
        checkLockoutStatus(emailHash, purpose);
        
        String otpHash = encryptionService.hash(otpCode);
        
        List<Otp> otps = otpRepository.findByEmailHashAndPurposeAndUsedFalse(emailHash, purpose);
        
        if (otps.isEmpty()) {
            logger.warn("OTP not found or already used");
            recordFailedAttempt(emailHash, purpose);
            return false;
        }
        
        Optional<Otp> otpOpt = otps.stream()
                .filter(otp -> otpHash.equals(otp.getOtpHash()))
                .findFirst();
        
        if (otpOpt.isEmpty()) {
            logger.warn("Invalid OTP code");
            recordFailedAttempt(emailHash, purpose);
            return false;
        }
        
        Otp otp = otpOpt.get();
        
        if (otp.isExpired()) {
            logger.warn("OTP expired");
            recordFailedAttempt(emailHash, purpose);
            return false;
        }
        
        otp.setUsed(true);
        otpRepository.save(otp);
        resetFailedAttempts(emailHash, purpose);
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
    
    /**
     * Clean up expired lockout entries (scheduled daily at 2 AM)
     * Removes entries where lockout has expired and entries older than 7 days
     */
    @Scheduled(cron = "0 0 2 * * ?") // Run daily at 2:00 AM
    public void cleanupExpiredLockouts() {
        logger.debug("Cleaning up expired OTP verification lockouts");
        LocalDateTime now = LocalDateTime.now();
        verificationAttemptRepository.deleteByLockoutUntilBefore(now);
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
        verificationAttemptRepository.deleteByUpdatedAtBefore(cutoffDate);
        logger.info("Expired OTP verification lockouts cleaned up");
    }
}

