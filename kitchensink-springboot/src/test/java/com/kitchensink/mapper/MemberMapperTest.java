package com.kitchensink.mapper;

import com.kitchensink.dto.MemberRequestDTO;
import com.kitchensink.dto.MemberResponseDTO;
import com.kitchensink.model.Member;
import com.kitchensink.service.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberMapper Unit Tests")
class MemberMapperTest {

    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private MemberMapper memberMapper;

    private MemberRequestDTO requestDTO;
    private Member member;
    private String emailHash = "email-hash-123";
    private String phoneHash = "phone-hash-123";
    private String emailEncrypted = "v1:encrypted-email";
    private String phoneEncrypted = "v1:encrypted-phone";

    @BeforeEach
    void setUp() {
        requestDTO = new MemberRequestDTO();
        requestDTO.setName("John Doe");
        requestDTO.setEmail("john.doe@example.com");
        requestDTO.setPhoneNumber("1234567890");
        requestDTO.setStatus("ACTIVE");

        member = new Member();
        member.setId("test-id");
        member.setName("John Doe");
        member.setEmailHash(emailHash);
        member.setPhoneNumberHash(phoneHash);
        member.setEmailEncrypted(emailEncrypted);
        member.setPhoneNumberEncrypted(phoneEncrypted);
        member.setRegistrationDate(LocalDateTime.now());
        member.setStatus("ACTIVE");

        // Default mock behaviors - using lenient to avoid unnecessary stubbing errors
        lenient().when(encryptionService.hash(anyString())).thenAnswer(invocation -> {
            String input = invocation.getArgument(0);
            if (input.contains("@")) return emailHash;
            return phoneHash;
        });
        lenient().when(encryptionService.encrypt(anyString())).thenAnswer(invocation -> {
            String input = invocation.getArgument(0);
            if (input.contains("@")) return emailEncrypted;
            return phoneEncrypted;
        });
        lenient().when(encryptionService.decrypt(anyString())).thenAnswer(invocation -> {
            String input = invocation.getArgument(0);
            if (input.contains("email")) return "john.doe@example.com";
            return "1234567890";
        });
        lenient().when(encryptionService.needsReEncryption(anyString())).thenReturn(false);
    }

    @Test
    @DisplayName("Should convert DTO to Entity")
    void testToEntity() {
        // When
        Member result = memberMapper.toEntity(requestDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("John Doe");
        assertThat(result.getEmailHash()).isEqualTo(emailHash);
        assertThat(result.getPhoneNumberHash()).isEqualTo(phoneHash);
        assertThat(result.getEmailEncrypted()).isEqualTo(emailEncrypted);
        assertThat(result.getPhoneNumberEncrypted()).isEqualTo(phoneEncrypted);
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        verify(encryptionService).hash("john.doe@example.com");
        verify(encryptionService).hash("1234567890");
        verify(encryptionService).encrypt("john.doe@example.com");
        verify(encryptionService).encrypt("1234567890");
    }

    @Test
    @DisplayName("Should return null when DTO is null")
    void testToEntity_NullDTO() {
        // When
        Member result = memberMapper.toEntity(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should convert Entity to Response DTO")
    void testToResponseDTO() {
        // When
        MemberResponseDTO result = memberMapper.toResponseDTO(member);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("test-id");
        assertThat(result.getName()).isEqualTo("John Doe");
        assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(result.getPhoneNumber()).isEqualTo("1234567890");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getRegistrationDate()).isNotNull();
        verify(encryptionService).decrypt(emailEncrypted);
        verify(encryptionService).decrypt(phoneEncrypted);
    }

    @Test
    @DisplayName("Should return null when Entity is null")
    void testToResponseDTO_NullEntity() {
        // When
        MemberResponseDTO result = memberMapper.toResponseDTO(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should update Entity from DTO")
    void testUpdateEntityFromDTO() {
        // Given
        MemberRequestDTO updateDTO = new MemberRequestDTO();
        updateDTO.setName("Jane Doe");
        updateDTO.setEmail("jane.doe@example.com");
        updateDTO.setPhoneNumber("9876543210");
        updateDTO.setStatus("INACTIVE");

        when(encryptionService.hash("jane.doe@example.com")).thenReturn("new-email-hash");
        when(encryptionService.hash("9876543210")).thenReturn("new-phone-hash");
        when(encryptionService.encrypt("jane.doe@example.com")).thenReturn("v1:new-encrypted-email");
        when(encryptionService.encrypt("9876543210")).thenReturn("v1:new-encrypted-phone");

        // When
        memberMapper.updateEntityFromDTO(member, updateDTO);

        // Then
        assertThat(member.getName()).isEqualTo("Jane Doe");
        assertThat(member.getEmailHash()).isEqualTo("new-email-hash");
        assertThat(member.getPhoneNumberHash()).isEqualTo("new-phone-hash");
        assertThat(member.getEmailEncrypted()).isEqualTo("v1:new-encrypted-email");
        assertThat(member.getPhoneNumberEncrypted()).isEqualTo("v1:new-encrypted-phone");
        assertThat(member.getStatus()).isEqualTo("INACTIVE");
        // Registration date should be preserved
        assertThat(member.getRegistrationDate()).isNotNull();
    }

    @Test
    @DisplayName("Should not update when Entity or DTO is null")
    void testUpdateEntityFromDTO_NullInputs() {
        // Given
        Member originalMember = new Member();
        originalMember.setName("Original Name");

        // When
        memberMapper.updateEntityFromDTO(null, requestDTO);
        memberMapper.updateEntityFromDTO(originalMember, null);

        // Then
        assertThat(originalMember.getName()).isEqualTo("Original Name");
        verify(encryptionService, never()).hash(anyString());
    }

    @Test
    @DisplayName("Should handle null email and phone in DTO")
    void testToEntity_NullEmailPhone() {
        // Given
        requestDTO.setEmail(null);
        requestDTO.setPhoneNumber(null);

        // When
        Member result = memberMapper.toEntity(requestDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("John Doe");
        assertThat(result.getEmailHash()).isNull();
        assertThat(result.getPhoneNumberHash()).isNull();
        verify(encryptionService, never()).hash(anyString());
        verify(encryptionService, never()).encrypt(anyString());
    }
}

