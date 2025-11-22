package com.kitchensink.controller;

import com.kitchensink.dto.MemberRequestDTO;
import com.kitchensink.dto.MemberResponseDTO;
import com.kitchensink.dto.Response;
import com.kitchensink.exception.ResourceConflictException;
import com.kitchensink.exception.ResourceNotFoundException;
import com.kitchensink.mapper.MemberMapper;
import com.kitchensink.model.Member;
import com.kitchensink.service.MemberService;
import com.kitchensink.util.CorrelationIdUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberController Unit Tests")
class MemberControllerTest {

    @Mock
    private MemberService memberService;

    @Mock
    private MemberMapper memberMapper;

    @InjectMocks
    private MemberController memberController;

    private MemberRequestDTO requestDTO;
    private Member member;
    private MemberResponseDTO responseDTO;
    private String memberId;

    @BeforeEach
    void setUp() {
        memberId = "test-id-123";
        
        requestDTO = new MemberRequestDTO();
        requestDTO.setName("John Doe");
        requestDTO.setEmail("john.doe@example.com");
        requestDTO.setPhoneNumber("1234567890");
        requestDTO.setStatus("ACTIVE");

        member = new Member();
        member.setId(memberId);
        member.setName("John Doe");
        member.setEmailHash("email-hash");
        member.setPhoneNumberHash("phone-hash");
        member.setEmailEncrypted("v1:encrypted-email");
        member.setPhoneNumberEncrypted("v1:encrypted-phone");
        member.setRegistrationDate(LocalDateTime.now());
        member.setStatus("ACTIVE");

        responseDTO = new MemberResponseDTO();
        responseDTO.setId(memberId);
        responseDTO.setName("John Doe");
        responseDTO.setEmail("john.doe@example.com");
        responseDTO.setPhoneNumber("1234567890");
        responseDTO.setRegistrationDate(LocalDateTime.now());
        responseDTO.setStatus("ACTIVE");

        // Set correlation ID for tests using MDC
        MDC.put("correlationId", "test-correlation-id");
    }

    @Test
    @DisplayName("Should get all members with pagination")
    void testGetAllMembers() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Member> memberPage = new PageImpl<>(Arrays.asList(member), pageable, 1);
        Page<MemberResponseDTO> dtoPage = new PageImpl<>(Arrays.asList(responseDTO), pageable, 1);
        
        when(memberService.getAllMembers(pageable)).thenReturn(memberPage);
        when(memberMapper.toResponseDTOPage(memberPage)).thenReturn(dtoPage);

        // When
        ResponseEntity<Response<Page<MemberResponseDTO>>> response = memberController.getAllMembers(pageable);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getContent()).hasSize(1);
        assertThat(response.getBody().getCorrelationId()).isEqualTo("test-correlation-id");
        verify(memberService).getAllMembers(pageable);
    }

    @Test
    @DisplayName("Should get member by ID")
    void testGetMemberById() {
        // Given
        when(memberService.getMemberById(memberId)).thenReturn(member);
        when(memberMapper.toResponseDTO(member)).thenReturn(responseDTO);

        // When
        ResponseEntity<Response<MemberResponseDTO>> response = memberController.getMemberById(memberId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getId()).isEqualTo(memberId);
        assertThat(response.getBody().getCorrelationId()).isEqualTo("test-correlation-id");
        verify(memberService).getMemberById(memberId);
    }

    @Test
    @DisplayName("Should create member successfully")
    void testCreateMember() {
        // Given
        when(memberService.createMember(requestDTO)).thenReturn(member);
        when(memberMapper.toResponseDTO(member)).thenReturn(responseDTO);

        // When
        ResponseEntity<Response<MemberResponseDTO>> response = memberController.createMember(requestDTO);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getId()).isEqualTo(memberId);
        assertThat(response.getBody().getMessage()).contains("created successfully");
        assertThat(response.getBody().getCorrelationId()).isEqualTo("test-correlation-id");
        verify(memberService).createMember(requestDTO);
    }

    @Test
    @DisplayName("Should update member successfully")
    void testUpdateMember() {
        // Given
        when(memberService.updateMember(memberId, requestDTO)).thenReturn(member);
        when(memberMapper.toResponseDTO(member)).thenReturn(responseDTO);

        // When
        ResponseEntity<Response<MemberResponseDTO>> response = memberController.updateMember(memberId, requestDTO);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getId()).isEqualTo(memberId);
        assertThat(response.getBody().getMessage()).contains("updated successfully");
        verify(memberService).updateMember(memberId, requestDTO);
    }

    @Test
    @DisplayName("Should delete member successfully")
    void testDeleteMember() {
        // Given
        when(memberService.deleteMember(memberId)).thenReturn(member);
        when(memberMapper.toResponseDTO(member)).thenReturn(responseDTO);

        // When
        ResponseEntity<Response<MemberResponseDTO>> response = memberController.deleteMember(memberId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getId()).isEqualTo(memberId);
        assertThat(response.getBody().getMessage()).contains("deleted successfully");
        verify(memberService).deleteMember(memberId);
    }

    @Test
    @DisplayName("Should search members by name or domain")
    void testSearchMembers() {
        // Given
        String name = "John";
        List<Member> members = Arrays.asList(member);
        List<MemberResponseDTO> dtos = Arrays.asList(responseDTO);
        
        when(memberService.searchMembers(name, null)).thenReturn(members);
        when(memberMapper.toResponseDTOList(members)).thenReturn(dtos);

        // When
        ResponseEntity<Response<List<MemberResponseDTO>>> response = memberController.searchMembers(name, null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).hasSize(1);
        verify(memberService).searchMembers(name, null);
    }
}

