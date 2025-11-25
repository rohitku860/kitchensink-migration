package com.kitchensink.repository;

import com.kitchensink.model.UpdateRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UpdateRequestRepository extends MongoRepository<UpdateRequest, String> {
    
    List<UpdateRequest> findByUserIdOrderByRequestedAtDesc(String userId);
    
    List<UpdateRequest> findByStatusOrderByRequestedAtDesc(String status);
    
    Optional<UpdateRequest> findByUserIdAndFieldNameAndStatus(String userId, String fieldName, String status);
}

