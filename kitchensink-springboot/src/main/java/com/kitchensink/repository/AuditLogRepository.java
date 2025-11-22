package com.kitchensink.repository;

import com.kitchensink.model.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
    
    List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, String entityId);
    
    List<AuditLog> findByActionAndTimestampBetween(String action, LocalDateTime start, LocalDateTime end);
    
    List<AuditLog> findByEntityTypeOrderByTimestampDesc(String entityType);
}

