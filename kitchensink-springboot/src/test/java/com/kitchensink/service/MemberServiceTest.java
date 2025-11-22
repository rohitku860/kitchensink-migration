package com.kitchensink.service;

import com.kitchensink.dto.MemberRequestDTO;
import com.kitchensink.exception.ResourceConflictException;
import com.kitchensink.exception.ResourceNotFoundException;
import com.kitchensink.mapper.MemberMapper;
import com.kitchensink.model.Member;
import com.kitchensink.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService Unit Tests")
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private InputSanitizationService sanitizationService;

    @Mock
    private MemberMapper memberMapper;

    @InjectMocks
    private MemberService memberService;

    private MemberRequestDTO requestDTO;
    private Member member;
    private String memberId;
    private String emailHash;
    private String phoneNumberHash;

    @BeforeEach
    void setUp() {
        memberId = "test-id-123";
        emailHash = "email-hash-123";
        phoneNumberHash = "phone-hash-123";

        requestDTO = new MemberRequestDTO();
        requestDTO.setName("John Doe");
        requestDTO.setEmail("john.doe@example.com");
        requestDTO.setPhoneNumber("1234567890");
        requestDTO.setStatus("ACTIVE");

        member = new Member();
        member.setId(memberId);
        member.setName("John Doe");
        member.setEmailHash(emailHash);
        member.setPhoneNumberHash(phoneNumberHash);
        member.setEmailEncrypted("v1:encrypted-email");
        member.setPhoneNumberEncrypted("v1:encrypted-phone");
        member.setRegistrationDate(LocalDateTime.now());
        member.setStatus("ACTIVE");

        // Default mock behaviors - using lenient to avoid unnecessary stubbing errors
        lenient().when(encryptionService.hash(anyString())).thenAnswer(invocation -> {
            String input = invocation.getArgument(0);
            if (input.contains("@")) return emailHash;
            return phoneNumberHash;
        });
        lenient().when(sanitizationService.sanitizeForName(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(sanitizationService.sanitizeForEmail(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(sanitizationService.sanitizeForPhone(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Should create member successfully")
    void testCreateMember_Success() {
        // Given
        when(memberRepository.existsByEmailHash(emailHash)).thenReturn(false);
        when(memberRepository.existsByPhoneNumberHash(phoneNumberHash)).thenReturn(false);
        when(memberMapper.toEntity(requestDTO)).thenReturn(member);
        when(memberRepository.save(any(Member.class))).thenReturn(member);

        // When
        Member result = memberService.createMember(requestDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(memberId);
        assertThat(result.getName()).isEqualTo("John Doe");
        verify(memberRepository).existsByEmailHash(emailHash);
        verify(memberRepository).existsByPhoneNumberHash(phoneNumberHash);
        verify(memberRepository).save(any(Member.class));
        verify(auditService).logMemberCreated(member);
        verify(sanitizationService).sanitizeForName("John Doe");
        verify(sanitizationService).sanitizeForEmail("john.doe@example.com");
        verify(sanitizationService).sanitizeForPhone("1234567890");
    }

    @Test
    @DisplayName("Should throw ResourceConflictException when email already exists")
    void testCreateMember_EmailExists() {
        // Given
        when(memberRepository.existsByEmailHash(emailHash)).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> memberService.createMember(requestDTO))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Email already exists");
        verify(memberRepository, never()).save(any(Member.class));
        verify(auditService, never()).logMemberCreated(any());
    }

    @Test
    @DisplayName("Should throw ResourceConflictException when phone number already exists")
    void testCreateMember_PhoneExists() {
        // Given
        when(memberRepository.existsByEmailHash(emailHash)).thenReturn(false);
        when(memberRepository.existsByPhoneNumberHash(phoneNumberHash)).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> memberService.createMember(requestDTO))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Phone number already exists");
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("Should set default status to ACTIVE if not provided")
    void testCreateMember_DefaultStatus() {
        // Given
        requestDTO.setStatus(null);
        when(memberRepository.existsByEmailHash(emailHash)).thenReturn(false);
        when(memberRepository.existsByPhoneNumberHash(phoneNumberHash)).thenReturn(false);
        when(memberMapper.toEntity(requestDTO)).thenReturn(member);
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> {
            Member m = invocation.getArgument(0);
            if (m.getStatus() == null || m.getStatus().isEmpty()) {
                m.setStatus("ACTIVE");
            }
            return m;
        });

        // When
        Member result = memberService.createMember(requestDTO);

        // Then
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("Should handle DuplicateKeyException during creation")
    void testCreateMember_DuplicateKeyException() {
        // Given
        when(memberRepository.existsByEmailHash(emailHash)).thenReturn(false);
        when(memberRepository.existsByPhoneNumberHash(phoneNumberHash)).thenReturn(false);
        when(memberMapper.toEntity(requestDTO)).thenReturn(member);
        when(memberRepository.save(any(Member.class)))
                .thenThrow(new DuplicateKeyException("Duplicate key: emailHash"));

        // When/Then
        assertThatThrownBy(() -> memberService.createMember(requestDTO))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    @DisplayName("Should get all members with pagination")
    void testGetAllMembers() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Member> members = Arrays.asList(member);
        Page<Member> page = new PageImpl<>(members, pageable, 1);
        when(memberRepository.findAllByOrderByNameAsc(pageable)).thenReturn(page);

        // When
        Page<Member> result = memberService.getAllMembers(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(member);
        verify(memberRepository).findAllByOrderByNameAsc(pageable);
    }

    @Test
    @DisplayName("Should get member by ID successfully")
    void testGetMemberById_Success() {
        // Given
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        // When
        Member result = memberService.getMemberById(memberId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(memberId);
        verify(memberRepository).findById(memberId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when member not found")
    void testGetMemberById_NotFound() {
        // Given
        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> memberService.getMemberById(memberId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Member")
                .hasMessageContaining(memberId);
    }

    @Test
    @DisplayName("Should update member successfully")
    void testUpdateMember_Success() {
        // Given
        MemberRequestDTO updateDTO = new MemberRequestDTO();
        updateDTO.setName("Jane Doe");
        updateDTO.setEmail("jane.doe@example.com");
        updateDTO.setPhoneNumber("9876543210");

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(encryptionService.hash("jane.doe@example.com")).thenReturn("new-email-hash");
        when(encryptionService.hash("9876543210")).thenReturn("new-phone-hash");
        when(memberRepository.existsByEmailHash("new-email-hash")).thenReturn(false);
        when(memberRepository.existsByPhoneNumberHash("new-phone-hash")).thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenReturn(member);

        // When
        Member result = memberService.updateMember(memberId, updateDTO);

        // Then
        assertThat(result).isNotNull();
        verify(memberRepository).findById(memberId);
        verify(memberMapper).updateEntityFromDTO(member, updateDTO);
        verify(memberRepository).save(member);
        verify(auditService).logMemberUpdated(any(), any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent member")
    void testUpdateMember_NotFound() {
        // Given
        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> memberService.updateMember(memberId, requestDTO))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("Should throw ResourceConflictException when updating with existing email")
    void testUpdateMember_EmailConflict() {
        // Given
        MemberRequestDTO updateDTO = new MemberRequestDTO();
        updateDTO.setEmail("existing@example.com");
        updateDTO.setPhoneNumber("1234567890");

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(encryptionService.hash("existing@example.com")).thenReturn("existing-email-hash");
        when(encryptionService.hash("1234567890")).thenReturn(phoneNumberHash);
        when(memberRepository.existsByEmailHash("existing-email-hash")).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> memberService.updateMember(memberId, updateDTO))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    @DisplayName("Should delete member successfully")
    void testDeleteMember_Success() {
        // Given
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        doNothing().when(memberRepository).deleteById(memberId);

        // When
        Member result = memberService.deleteMember(memberId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(memberId);
        verify(memberRepository).findById(memberId);
        verify(memberRepository).deleteById(memberId);
        verify(auditService).logMemberDeleted(member);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent member")
    void testDeleteMember_NotFound() {
        // Given
        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> memberService.deleteMember(memberId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(memberRepository, never()).deleteById(anyString());
    }

    @Test
    @DisplayName("Should search members by name")
    void testSearchMembersByName() {
        // Given
        String searchText = "John";
        List<Member> members = Arrays.asList(member);
        when(memberRepository.searchByName(".*John.*")).thenReturn(members);

        // When
        List<Member> result = memberService.searchMembersByName(searchText);

        // Then
        assertThat(result).hasSize(1);
        verify(memberRepository).searchByName(".*John.*");
    }

    @Test
    @DisplayName("Should search members by name or domain")
    void testSearchMembers() {
        // Given
        String name = "John";
        List<Member> members = Arrays.asList(member);
        when(sanitizationService.sanitizeForName(name)).thenReturn(name);
        when(memberRepository.searchByName(".*John.*")).thenReturn(members);

        // When
        List<Member> result = memberService.searchMembers(name);

        // Then
        assertThat(result).hasSize(1);
        verify(sanitizationService).sanitizeForName(name);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when no search parameters provided")
    void testSearchMembers_NoParameters() {
        // When/Then
        assertThatThrownBy(() -> memberService.searchMembers(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Either 'name' or 'domain' parameter must be provided");
    }

    @Test
    @DisplayName("Should check if email exists")
    void testEmailExists() {
        // Given
        when(memberRepository.existsByEmailHash(emailHash)).thenReturn(true);

        // When
        boolean exists = memberService.emailExists("test@example.com");

        // Then
        assertThat(exists).isTrue();
        verify(encryptionService).hash("test@example.com");
        verify(memberRepository).existsByEmailHash(emailHash);
    }

    @Test
    @DisplayName("Should check if phone number exists")
    void testPhoneNumberExists() {
        // Given
        when(memberRepository.existsByPhoneNumberHash(phoneNumberHash)).thenReturn(true);

        // When
        boolean exists = memberService.phoneNumberExists("1234567890");

        // Then
        assertThat(exists).isTrue();
        verify(encryptionService).hash("1234567890");
        verify(memberRepository).existsByPhoneNumberHash(phoneNumberHash);
    }

    @Test
    @DisplayName("Should get member by email")
    void testGetMemberByEmail() {
        // Given
        when(memberRepository.findByEmailHash(emailHash)).thenReturn(Optional.of(member));

        // When
        Optional<Member> result = memberService.getMemberByEmail("test@example.com");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(member);
        verify(encryptionService).hash("test@example.com");
        verify(memberRepository).findByEmailHash(emailHash);
    }

    @Test
    @DisplayName("Should get member by phone number")
    void testGetMemberByPhoneNumber() {
        // Given
        when(memberRepository.findByPhoneNumberHash(phoneNumberHash)).thenReturn(Optional.of(member));

        // When
        Optional<Member> result = memberService.getMemberByPhoneNumber("1234567890");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(member);
        verify(encryptionService).hash("1234567890");
        verify(memberRepository).findByPhoneNumberHash(phoneNumberHash);
    }
}

