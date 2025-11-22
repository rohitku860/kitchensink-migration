package com.kitchensink.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kitchensink.dto.MemberRequestDTO;
import com.kitchensink.dto.MemberResponseDTO;
import com.kitchensink.dto.Response;
import com.kitchensink.mapper.MemberMapper;
import com.kitchensink.model.Member;
import com.kitchensink.service.MemberService;
import com.kitchensink.util.CorrelationIdUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/members")
@Tag(name = "Members", description = "Member Registration and Management API v1")
public class MemberController {
    
    private static final Logger logger = LoggerFactory.getLogger(MemberController.class);
    
    private final MemberService memberService;
    private final MemberMapper memberMapper;
    
    public MemberController(MemberService memberService, MemberMapper memberMapper) {
        this.memberService = memberService;
        this.memberMapper = memberMapper;
    }
    
    
    
    @GetMapping
    @Operation(summary = "Get all members", description = "Retrieve a paginated list of all members ordered by name")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list of members",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MemberResponseDTO.class)))
    public ResponseEntity<Response<Page<MemberResponseDTO>>> getAllMembers(
            @Parameter(description = "Pagination parameters (page, size, sort)") 
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        
    	logger.debug("Getting all members - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        
    	Page<MemberResponseDTO> page = memberMapper.toResponseDTOPage(memberService.getAllMembers(pageable));
        
    	logger.info("Retrieved page {} with {} members out of {} total", 
                page.getNumber(), page.getNumberOfElements(), page.getTotalElements());
        
        Response<Page<MemberResponseDTO>> response = Response.success(page, "Members retrieved successfully");
        response.setCorrelationId(CorrelationIdUtil.getCorrelationId());
        return ResponseEntity.ok(response);
    }
    
    
    
  
    @GetMapping("/{id}")
    @Operation(summary = "Get member by ID", description = "Retrieve a member by their unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Member found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MemberResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Member not found")
    })
    public ResponseEntity<Response<MemberResponseDTO>> getMemberById(
            @Parameter(description = "Member ID", required = true) @PathVariable String id) {
       
    	logger.debug("Getting member with ID: {}", id);
        Member member = memberService.getMemberById(id);
        MemberResponseDTO responseDTO = memberMapper.toResponseDTO(member);
        Response<MemberResponseDTO> response = Response.success(responseDTO, "Member retrieved successfully");
        response.setCorrelationId(CorrelationIdUtil.getCorrelationId());
        return ResponseEntity.ok(response);
    }
    
    
    
    
    @PostMapping
    @Operation(summary = "Create a new member", description = "Register a new member with validation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Member created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MemberResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "Email or phone number already exists")
    })
    public ResponseEntity<Response<MemberResponseDTO>> createMember(@Valid @RequestBody MemberRequestDTO requestDTO) {
        logger.debug("Creating new member with email: {}", requestDTO.getEmail());
        
        Member createdMember = memberService.createMember(requestDTO);
        MemberResponseDTO responseDTO = memberMapper.toResponseDTO(createdMember);
        logger.info("Successfully created member with ID: {}", responseDTO.getId());
        
        Response<MemberResponseDTO> response = Response.success(responseDTO, "Member created successfully");
        response.setCorrelationId(CorrelationIdUtil.getCorrelationId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    
    @PutMapping("/{id}")
    @Operation(summary = "Update a member", description = "Update an existing member's information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Member updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MemberResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Member not found"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "Email or phone number already exists")
    })
    public ResponseEntity<Response<MemberResponseDTO>> updateMember(
            @Parameter(description = "Member ID", required = true) @PathVariable String id, 
            @Valid @RequestBody MemberRequestDTO requestDTO) {
        logger.debug("Updating member with ID: {}", id);
        
        Member updatedMember = memberService.updateMember(id, requestDTO);
        MemberResponseDTO responseDTO = memberMapper.toResponseDTO(updatedMember);
        logger.info("Successfully updated member with ID: {}", id);
        
        Response<MemberResponseDTO> response = Response.success(responseDTO, "Member updated successfully");
        response.setCorrelationId(CorrelationIdUtil.getCorrelationId());
        return ResponseEntity.ok(response);
    }
    
   
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a member", description = "Delete a member by their ID and return the deleted member")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Member deleted successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MemberResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Member not found")
    })
    public ResponseEntity<Response<MemberResponseDTO>> deleteMember(
            @Parameter(description = "Member ID", required = true) @PathVariable String id) {
       
    	logger.debug("Deleting member with ID: {}", id);
        Member deletedMember = memberService.deleteMember(id);
        MemberResponseDTO responseDTO = memberMapper.toResponseDTO(deletedMember);
        logger.info("Successfully deleted member with ID: {}", id);
        
        Response<MemberResponseDTO> response = Response.success(responseDTO, "Member deleted successfully");
        response.setCorrelationId(CorrelationIdUtil.getCorrelationId());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/search")
    @Operation(summary = "Search members", description = "Search members by name (case-insensitive)")
    @ApiResponse(responseCode = "200", description = "Search results",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MemberResponseDTO.class)))
    public ResponseEntity<Response<List<MemberResponseDTO>>> searchMembers(
            @Parameter(description = "Search by name (case-insensitive)", required = true) @RequestParam String name) {
        logger.debug("Searching members by name: {}", name);
        
        List<Member> members = memberService.searchMembers(name);
        List<MemberResponseDTO> responseDTOs = memberMapper.toResponseDTOList(members);
        logger.info("Found {} members matching search criteria", responseDTOs.size());
        
        Response<List<MemberResponseDTO>> response = Response.success(responseDTOs, 
                String.format("Found %d members", responseDTOs.size()));
        response.setCorrelationId(CorrelationIdUtil.getCorrelationId());
        return ResponseEntity.ok(response);
    }
}

