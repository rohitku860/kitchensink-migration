package com.kitchensink.repository;

import com.kitchensink.model.Otp;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OtpRepository extends MongoRepository<Otp, String> {
    
    List<Otp> findByEmailHashAndPurposeAndUsedFalse(String emailHash, String purpose);
    
    List<Otp> findByExpiresAtBefore(LocalDateTime now);
    
    void deleteByExpiresAtBefore(LocalDateTime now);
    
    /**
     * Count OTP attempts for an email and purpose within a time window
     */
    long countByEmailHashAndPurposeAndCreatedAtAfter(String emailHash, String purpose, LocalDateTime after);
}

