package com.kitchensink.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.kitchensink.dto.MemberRequestDTO;
import com.kitchensink.exception.ResourceConflictException;
import com.kitchensink.exception.ResourceNotFoundException;
import com.kitchensink.mapper.MemberMapper;
import com.kitchensink.model.Member;
import com.kitchensink.repository.MemberRepository;

@Service
public class MemberService {
    
    private static final Logger logger = LoggerFactory.getLogger(MemberService.class);
    private final MemberRepository memberRepository;
    private final AuditService auditService;
    private final EncryptionService encryptionService;
    private final InputSanitizationService sanitizationService;
    private final MemberMapper memberMapper;
    
    public MemberService(MemberRepository memberRepository, AuditService auditService, 
                        EncryptionService encryptionService, InputSanitizationService sanitizationService,
                        MemberMapper memberMapper) {
        this.memberRepository = memberRepository;
        this.auditService = auditService;
        this.encryptionService = encryptionService;
        this.sanitizationService = sanitizationService;
        this.memberMapper = memberMapper;
    }
    
    public Member createMember(MemberRequestDTO requestDTO) {
        logger.debug("Creating member with email: [REDACTED]");
        
        sanitizeRequestDTO(requestDTO);
        
        Member member = memberMapper.toEntity(requestDTO);
        
        if (member.getRegistrationDate() == null) {
            member.setRegistrationDate(LocalDateTime.now());
        }
        
        if (member.getStatus() == null || member.getStatus().isEmpty()) {
            member.setStatus("ACTIVE");
        }
        
        try {
            // Single query - MongoDB unique indexes will enforce uniqueness automatically
            // No need for pre-checks - MongoDB will throw DuplicateKeyException if duplicate exists
            Member saved = memberRepository.save(member);
            logger.info("Member created successfully with ID: {}", saved.getId());
            auditService.logMemberCreated(saved);
            return saved;
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // MongoDB unique index violation - parse error to determine which field
            logger.warn("Duplicate key violation during member creation: {}", e.getMessage());
            String message = e.getMessage();
            String field = "error";
            String errorMessage = "Duplicate key violation";
            
            if (message != null) {
                String lowerMessage = message.toLowerCase();
                if (lowerMessage.contains("emailhash") || lowerMessage.contains("email_hash_unique_idx")) {
                    field = "email";
                    errorMessage = "Email already exists";
                } else if (lowerMessage.contains("phonenumberhash") || lowerMessage.contains("phonenumber_hash_unique_idx")) {
                    field = "phoneNumber";
                    errorMessage = "Phone number already exists";
                }
            }
            throw new ResourceConflictException(errorMessage, field);
        }
    }
    
    public Page<Member> getAllMembers(Pageable pageable) {
        logger.debug("Fetching members - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<Member> page = memberRepository.findAllByOrderByNameAsc(pageable);
        logger.info("Retrieved {} members out of {} total", page.getNumberOfElements(), page.getTotalElements());
        return page;
    }
    
    @Cacheable(value = "memberById", key = "#id")
    public Member getMemberById(String id) {
        logger.debug("Fetching member by ID: {}", id);
        return memberRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Member not found with ID: {}", id);
                    return new ResourceNotFoundException("Member", id);
                });
    }
    
    /**
     * Fast O(1) lookup by email using hash (works across all key versions!)
     */
    public Optional<Member> getMemberByEmail(String email) {
        logger.debug("Fetching member by email: [REDACTED]");
        String emailHash = encryptionService.hash(email);
        return memberRepository.findByEmailHash(emailHash);
    }
    
    /**
     * Fast O(1) lookup by phone number using hash (works across all key versions!)
     */
    public Optional<Member> getMemberByPhoneNumber(String phoneNumber) {
        logger.debug("Fetching member by phone number: [REDACTED]");
        String phoneNumberHash = encryptionService.hash(phoneNumber);
        return memberRepository.findByPhoneNumberHash(phoneNumberHash);
    }
    
    @CacheEvict(value = "memberById", key = "#id")
    public Member updateMember(String id, MemberRequestDTO requestDTO) {
        logger.debug("Updating member with ID: {}", id);
        
        Member existingMember = memberRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Member not found for update with ID: {}", id);
                    return new ResourceNotFoundException("Member", id);
                });
        
        sanitizeRequestDTO(requestDTO);
        
        // Update existing member (preserves fields not in DTO like registrationDate)
        // Don't create new Member - update existing one to preserve all fields
        memberMapper.updateEntityFromDTO(existingMember, requestDTO);
        
        try {
            // Single query - MongoDB unique indexes will enforce uniqueness automatically
            // No need for pre-checks - MongoDB will throw DuplicateKeyException if duplicate exists
            Member updated = memberRepository.save(existingMember);
            logger.info("Member updated successfully with ID: {}", id);
            auditService.logMemberUpdated(existingMember, updated);
            // Cache eviction happens via @CacheEvict on this method
            return updated;
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // MongoDB unique index violation - parse error to determine which field
            logger.warn("Duplicate key violation during member update: {}", e.getMessage());
            String message = e.getMessage();
            String field = "error";
            String errorMessage = "Duplicate key violation";
            
            if (message != null) {
                String lowerMessage = message.toLowerCase();
                if (lowerMessage.contains("emailhash") || lowerMessage.contains("email_hash_unique_idx")) {
                    field = "email";
                    errorMessage = "Email already exists";
                } else if (lowerMessage.contains("phonenumberhash") || lowerMessage.contains("phonenumber_hash_unique_idx")) {
                    field = "phoneNumber";
                    errorMessage = "Phone number already exists";
                }
            }
            throw new ResourceConflictException(errorMessage, field);
        }
    }
    
    @CacheEvict(value = "memberById", key = "#id")
    public Member deleteMember(String id) {
        logger.debug("Deleting member with ID: {}", id);
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Member not found for deletion with ID: {}", id);
                    return new ResourceNotFoundException("Member", id);
                });
        memberRepository.deleteById(id);
        logger.info("Member deleted successfully with ID: {}", id);
        auditService.logMemberDeleted(member);
        // Cache eviction happens via @CacheEvict on this method
        return member;
    }
    
    public List<Member> searchMembersByName(String searchText) {
        logger.debug("Searching members by name: {}", searchText);
        String pattern = ".*" + searchText + ".*";
        List<Member> results = memberRepository.searchByName(pattern);
        logger.info("Found {} members matching name: {}", results.size(), searchText);
        return results;
    }
    
    public List<Member> findMembersByNameContaining(String name) {
        logger.debug("Finding members with name containing: {}", name);
        return memberRepository.findByNameContainingIgnoreCaseOrderByNameAsc(name);
    }
    
    public List<Member> findMembersByEmailDomain(String domain) {
        logger.debug("Finding members with email domain: {}", domain);
        List<Member> results = memberRepository.findByEmailContainingOrderByNameAsc(domain);
        logger.info("Found {} members with email domain: {}", results.size(), domain);
        return results;
    }
    
    /**
     * Find member by exact email using hash-based lookup (O(1) performance)
     */
    public Optional<Member> findMemberByEmail(String email) {
        logger.debug("Finding member by exact email: [REDACTED]");
        String emailHash = encryptionService.hash(email);
        return memberRepository.findByEmailHash(emailHash);
    }
    
    public List<Member> searchMembers(String name) {
        logger.debug("Searching members by name: {}", name);
        
        if (name == null || name.isEmpty()) {
            logger.warn("Name parameter is required for search");
            throw new IllegalArgumentException("Name parameter is required for search");
        }
        
        String sanitizedName = sanitizationService.sanitizeForName(name);
        return searchMembersByName(sanitizedName);
    }
    
    private void sanitizeRequestDTO(MemberRequestDTO dto) {
        if (dto.getName() != null) {
            dto.setName(sanitizationService.sanitizeForName(dto.getName()));
        }
        if (dto.getEmail() != null) {
            dto.setEmail(sanitizationService.sanitizeForEmail(dto.getEmail()));
        }
        if (dto.getPhoneNumber() != null) {
            dto.setPhoneNumber(sanitizationService.sanitizeForPhone(dto.getPhoneNumber()));
        }
    }
    
    /**
     * Fast O(1) duplicate check using hash
     */
    public boolean emailExists(String email) {
        logger.debug("Checking if email exists: [REDACTED]");
        String emailHash = encryptionService.hash(email);
        return memberRepository.existsByEmailHash(emailHash);
    }
    
    /**
     * Fast O(1) duplicate check using hash
     */
    public boolean phoneNumberExists(String phoneNumber) {
        logger.debug("Checking if phone number exists: [REDACTED]");
        String phoneNumberHash = encryptionService.hash(phoneNumber);
        return memberRepository.existsByPhoneNumberHash(phoneNumberHash);
    }
    
}

