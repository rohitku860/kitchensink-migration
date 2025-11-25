package com.kitchensink.repository;

import com.kitchensink.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    
    Optional<User> findByEmailHash(String emailHash);
    
    boolean existsByEmailHash(String emailHash);
    
    Optional<User> findByPhoneNumberHash(String phoneNumberHash);
    
    boolean existsByPhoneNumberHash(String phoneNumberHash);
    
    Page<User> findAllByOrderByNameAsc(Pageable pageable);
    
    @Query("{ 'name': { $regex: ?0, $options: 'i' } }")
    java.util.List<User> searchByName(String pattern);
    
    java.util.List<User> findByNameContainingIgnoreCaseOrderByNameAsc(String name);
}

