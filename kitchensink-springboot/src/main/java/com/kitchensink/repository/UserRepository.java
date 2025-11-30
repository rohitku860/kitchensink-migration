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
    
    @Query("{ '_id': { $nin: ?0 } }")
    Page<User> findByIdNotInOrderByNameAsc(java.util.List<String> excludedIds, Pageable pageable);
    
    @Query("{ '_id': { $nin: ?0 } }")
    long countByIdNotIn(java.util.List<String> excludedIds);
    
    // Cursor-based pagination queries
    @Query("{ '_id': { $nin: ?0, $gt: ?1 } }")
    java.util.List<User> findByIdNotInAndIdGreaterThanOrderByIdAsc(
            java.util.List<String> excludedIds, String cursor, Pageable pageable);
    
    @Query("{ '_id': { $nin: ?0, $lt: ?1 } }")
    java.util.List<User> findByIdNotInAndIdLessThanOrderByIdDesc(
            java.util.List<String> excludedIds, String cursor, Pageable pageable);
    
    @Query("{ '_id': { $nin: ?0, $lt: ?1 } }")
    java.util.List<User> findByIdNotInAndIdLessThanOrderByIdAsc(
            java.util.List<String> excludedIds, String cursor, Pageable pageable);
    
    @Query("{ '_id': { $nin: ?0, $gt: ?1 } }")
    java.util.List<User> findByIdNotInAndIdGreaterThanOrderByNameAsc(
            java.util.List<String> excludedIds, String cursor, Pageable pageable);
    
    @Query("{ '_id': { $nin: ?0, $lt: ?1 } }")
    java.util.List<User> findByIdNotInAndIdLessThanOrderByNameDesc(
            java.util.List<String> excludedIds, String cursor, Pageable pageable);
    
    // Check if more records exist
    @Query("{ '_id': { $nin: ?0, $gt: ?1 } }")
    boolean existsByIdNotInAndIdGreaterThan(java.util.List<String> excludedIds, String cursor);
    
    @Query("{ '_id': { $nin: ?0, $lt: ?1 } }")
    boolean existsByIdNotInAndIdLessThan(java.util.List<String> excludedIds, String cursor);
}

