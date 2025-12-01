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
    
    @Query("{ 'name': { $regex: ?0, $options: 'i' } }")
    java.util.List<User> searchByName(String pattern);
    
    @Query("{ '_id': { $nin: ?0 } }")
    Page<User> findByIdNotInOrderByNameAsc(java.util.List<String> excludedIds, Pageable pageable);
    
    Page<User> findAllByOrderByNameAsc(Pageable pageable);
    
    @Query("{ '_id': { $nin: ?0, $gt: ?1 } }")
    java.util.List<User> findByIdNotInAndIdGreaterThanOrderByIdAsc(
            java.util.List<String> excludedIds, String cursor, Pageable pageable);
    
    @Query("{ '_id': { $nin: ?0, $lt: ?1 } }")
    java.util.List<User> findByIdNotInAndIdLessThanOrderByIdAsc(
            java.util.List<String> excludedIds, String cursor, Pageable pageable);
    
    java.util.List<User> findByIdLessThanOrderByIdAsc(String cursor, Pageable pageable);
    
    java.util.List<User> findByIdGreaterThanOrderByIdAsc(String cursor, Pageable pageable);
    
    // Check if more records exist
    @Query(value = "{ '_id': { $nin: ?0, $gt: ?1 } }", count = true)
    long countByIdNotInAndIdGreaterThan(java.util.List<String> excludedIds, String cursor);
    
    default boolean existsByIdNotInAndIdGreaterThan(java.util.List<String> excludedIds, String cursor) {
        return countByIdNotInAndIdGreaterThan(excludedIds, cursor) > 0;
    }
    
    @Query(value = "{ '_id': { $nin: ?0, $lt: ?1 } }", count = true)
    long countByIdNotInAndIdLessThan(java.util.List<String> excludedIds, String cursor);
    
    default boolean existsByIdNotInAndIdLessThan(java.util.List<String> excludedIds, String cursor) {
        return countByIdNotInAndIdLessThan(excludedIds, cursor) > 0;
    }
    
    @Query(value = "{ '_id': { $gt: ?0 } }", count = true)
    long countByIdGreaterThan(String cursor);
    
    default boolean existsByIdGreaterThan(String cursor) {
        return countByIdGreaterThan(cursor) > 0;
    }
    
    @Query(value = "{ '_id': { $lt: ?0 } }", count = true)
    long countByIdLessThan(String cursor);
    
    default boolean existsByIdLessThan(String cursor) {
        return countByIdLessThan(cursor) > 0;
    }
    
    @Query(value = "{ '_id': { $nin: ?0 } }", count = true)
    long countByIdNotIn(java.util.List<String> excludedIds);
    
    @Query("{ '_id': { $nin: ?0 }, $or: [ { 'name': { $gt: ?1 } }, { $and: [ { 'name': ?1 }, { '_id': { $gt: ?2 } } ] } ] }")
    java.util.List<User> findByIdNotInAndNameGreaterThanOrderByNameAsc(
            java.util.List<String> excludedIds, String lastName, String lastId, Pageable pageable);
    
    @Query("{ '_id': { $nin: ?0 }, $or: [ { 'name': { $lt: ?1 } }, { $and: [ { 'name': ?1 }, { '_id': { $lt: ?2 } } ] } ] }")
    java.util.List<User> findByIdNotInAndNameLessThanOrderByNameDesc(
            java.util.List<String> excludedIds, String firstName, String firstId, Pageable pageable);
    
    @Query("{ $or: [ { 'name': { $gt: ?0 } }, { $and: [ { 'name': ?0 }, { '_id': { $gt: ?1 } } ] } ] }")
    java.util.List<User> findByNameGreaterThanOrderByNameAsc(String lastName, String lastId, Pageable pageable);
    
    @Query("{ $or: [ { 'name': { $lt: ?0 } }, { $and: [ { 'name': ?0 }, { '_id': { $lt: ?1 } } ] } ] }")
    java.util.List<User> findByNameLessThanOrderByNameDesc(String firstName, String firstId, Pageable pageable);
    
    @Query(value = "{ '_id': { $nin: ?0 }, $or: [ { 'name': { $gt: ?1 } }, { 'name': ?1, '_id': { $gt: ?2 } } ] }", count = true)
    long countByIdNotInAndNameGreaterThan(java.util.List<String> excludedIds, String lastName, String lastId);
    
    default boolean existsByIdNotInAndNameGreaterThan(java.util.List<String> excludedIds, String lastName, String lastId) {
        return countByIdNotInAndNameGreaterThan(excludedIds, lastName, lastId) > 0;
    }
    
    @Query(value = "{ '_id': { $nin: ?0 }, $or: [ { 'name': { $lt: ?1 } }, { 'name': ?1, '_id': { $lt: ?2 } } ] }", count = true)
    long countByIdNotInAndNameLessThan(java.util.List<String> excludedIds, String firstName, String firstId);
    
    default boolean existsByIdNotInAndNameLessThan(java.util.List<String> excludedIds, String firstName, String firstId) {
        return countByIdNotInAndNameLessThan(excludedIds, firstName, firstId) > 0;
    }
    
    @Query(value = "{ $or: [ { 'name': { $gt: ?0 } }, { 'name': ?0, '_id': { $gt: ?1 } } ] }", count = true)
    long countByNameGreaterThan(String lastName, String lastId);
    
    default boolean existsByNameGreaterThan(String lastName, String lastId) {
        return countByNameGreaterThan(lastName, lastId) > 0;
    }
    
    @Query(value = "{ $or: [ { 'name': { $lt: ?0 } }, { 'name': ?0, '_id': { $lt: ?1 } } ] }", count = true)
    long countByNameLessThan(String firstName, String firstId);
    
    default boolean existsByNameLessThan(String firstName, String firstId) {
        return countByNameLessThan(firstName, firstId) > 0;
    }
}

