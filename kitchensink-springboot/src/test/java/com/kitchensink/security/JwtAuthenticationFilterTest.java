package com.kitchensink.security;

import com.kitchensink.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should authenticate with valid JWT token")
    void testDoFilterInternal_ValidToken() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-jwt-token");
        when(jwtUtil.extractUserId("valid-jwt-token")).thenReturn("user-1");
        when(jwtUtil.extractRole("valid-jwt-token")).thenReturn("USER");
        when(jwtUtil.validateToken("valid-jwt-token", "user-1")).thenReturn(true);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtUtil).validateToken("valid-jwt-token", "user-1");
    }

    @Test
    @DisplayName("Should skip authentication when no Authorization header")
    void testDoFilterInternal_NoHeader() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtUtil, never()).extractUserId(anyString());
    }

    @Test
    @DisplayName("Should skip authentication when header doesn't start with Bearer")
    void testDoFilterInternal_InvalidHeader() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Invalid token");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtUtil, never()).extractUserId(anyString());
    }

    @Test
    @DisplayName("Should skip authentication when token is invalid")
    void testDoFilterInternal_InvalidToken() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(jwtUtil.extractUserId("invalid-token")).thenReturn("user-1");
        when(jwtUtil.extractRole("invalid-token")).thenReturn("USER");
        when(jwtUtil.validateToken("invalid-token", "user-1")).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtUtil).validateToken("invalid-token", "user-1");
    }

    @Test
    @DisplayName("Should handle exception during token processing")
    void testDoFilterInternal_Exception() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenThrow(new RuntimeException("Token error"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should skip authentication when user ID is null")
    void testDoFilterInternal_NullUserId() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtUtil, never()).validateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("Should skip authentication when already authenticated")
    void testDoFilterInternal_AlreadyAuthenticated() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn("user-1");
        when(jwtUtil.extractRole("token")).thenReturn("USER");
        // Set authentication in context
        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(auth);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtUtil, never()).validateToken(anyString(), anyString());
    }
}

