package com.kitchensink.mapper;

import com.kitchensink.dto.MemberRequestDTO;
import com.kitchensink.dto.MemberResponseDTO;
import com.kitchensink.model.Member;
import com.kitchensink.service.EncryptionService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class MemberMapper {
    
    private final EncryptionService encryptionService;
    
    public MemberMapper(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }
    
    public Member toEntity(MemberRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        
        Member member = new Member();
        member.setName(dto.getName());
        
        if (dto.getEmail() != null) {
            member.setEmailHash(encryptionService.hash(dto.getEmail()));
            member.setEmailEncrypted(encryptionService.encrypt(dto.getEmail()));
        }
        if (dto.getPhoneNumber() != null) {
            member.setPhoneNumberHash(encryptionService.hash(dto.getPhoneNumber()));
            member.setPhoneNumberEncrypted(encryptionService.encrypt(dto.getPhoneNumber()));
        }
        
        if (dto.getStatus() != null && !dto.getStatus().isEmpty()) {
            member.setStatus(dto.getStatus());
        }
        
        return member;
    }
    
    public MemberResponseDTO toResponseDTO(Member member) {
        return toResponseDTO(member, "/kitchensink/v1/members");
    }
    
    public MemberResponseDTO toResponseDTO(Member member, String baseUrl) {
        if (member == null) {
            return null;
        }
        
        MemberResponseDTO dto = new MemberResponseDTO();
        dto.setId(member.getId());
        dto.setName(member.getName());
        
        // Decrypt PII only when returning to client
        // Also handles lazy re-encryption if data uses old key
        if (member.getEmailEncrypted() != null) {
            dto.setEmail(encryptionService.decrypt(member.getEmailEncrypted()));
            // Lazy re-encryption: if data uses old key, re-encrypt on next update
            if (encryptionService.needsReEncryption(member.getEmailEncrypted())) {
                // Note: Actual re-encryption should happen in service layer during update
                // This is just a check - the service layer should handle re-encryption
            }
        }
        
        if (member.getPhoneNumberEncrypted() != null) {
            dto.setPhoneNumber(encryptionService.decrypt(member.getPhoneNumberEncrypted()));
            // Lazy re-encryption: if data uses old key, re-encrypt on next update
            if (encryptionService.needsReEncryption(member.getPhoneNumberEncrypted())) {
                // Note: Actual re-encryption should happen in service layer during update
            }
        }
        
        // Set REST URL for this member (registrationDate and status are not exposed)
        if (member.getId() != null) {
            dto.setRestUrl(baseUrl + "/" + member.getId());
        }
        
        return dto;
    }
    
    public List<MemberResponseDTO> toResponseDTOList(List<Member> members) {
        return toResponseDTOList(members, "/kitchensink/v1/members");
    }
    
    public List<MemberResponseDTO> toResponseDTOList(List<Member> members, String baseUrl) {
        if (members == null) {
            return null;
        }
        
        return members.stream()
                .map(member -> toResponseDTO(member, baseUrl))
                .collect(Collectors.toList());
    }
    
    public Page<MemberResponseDTO> toResponseDTOPage(Page<Member> memberPage) {
        return toResponseDTOPage(memberPage, "/kitchensink/v1/members");
    }
    
    public Page<MemberResponseDTO> toResponseDTOPage(Page<Member> memberPage, String baseUrl) {
        if (memberPage == null) {
            return null;
        }
        
        return memberPage.map(member -> toResponseDTO(member, baseUrl));
    }
    
    public void updateEntityFromDTO(Member member, MemberRequestDTO dto) {
        if (member == null || dto == null) {
            return;
        }
        
        member.setName(dto.getName());
        
        // CRITICAL: Store both hash (for indexing/uniqueness) and encrypted (for security)
        if (dto.getEmail() != null) {
            member.setEmailHash(encryptionService.hash(dto.getEmail()));
            member.setEmailEncrypted(encryptionService.encrypt(dto.getEmail()));
        }
        if (dto.getPhoneNumber() != null) {
            member.setPhoneNumberHash(encryptionService.hash(dto.getPhoneNumber()));
            member.setPhoneNumberEncrypted(encryptionService.encrypt(dto.getPhoneNumber()));
        }
        
        if (dto.getStatus() != null) {
            member.setStatus(dto.getStatus());
        }
    }
}

