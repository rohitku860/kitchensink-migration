package com.kitchensink.repository;

import com.kitchensink.model.UserRole;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRoleRepository extends MongoRepository<UserRole, String> {
    
    Optional<UserRole> findByUserId(String userId);
    
    Optional<UserRole> findByUserIdAndActiveTrue(String userId);
    
    boolean existsByUserId(String userId);
}

