package com.kitchensink.repository;

import com.kitchensink.model.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends MongoRepository<Member, String> {
    
    Optional<Member> findByEmailHash(String emailHash);
    
    Optional<Member> findByPhoneNumberHash(String phoneNumberHash);
    
    boolean existsByEmailHash(String emailHash);
    
    boolean existsByPhoneNumberHash(String phoneNumberHash);
    
    Page<Member> findAllByOrderByNameAsc(Pageable pageable);
    
    List<Member> findAllByOrderByNameAsc();
    
    @Query("{ name: { $regex: ?0, $options: 'i' } }")
    List<Member> searchByName(String searchText);
    
    List<Member> findByNameContainingIgnoreCaseOrderByNameAsc(String name);
    
    List<Member> findByEmailContainingOrderByNameAsc(String domain);
    
   
}

