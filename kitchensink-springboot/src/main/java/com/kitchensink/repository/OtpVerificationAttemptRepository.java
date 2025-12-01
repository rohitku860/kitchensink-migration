package com.kitchensink.repository;

import com.kitchensink.model.OtpVerificationAttempt;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpVerificationAttemptRepository extends MongoRepository<OtpVerificationAttempt, String> {
    
    Optional<OtpVerificationAttempt> findByEmailHashAndPurpose(String emailHash, String purpose);
    
    void deleteByLockoutUntilBefore(LocalDateTime now);
    
    void deleteByUpdatedAtBefore(LocalDateTime cutoffDate);
}

