package com.kitchensink.security;

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
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.PrintWriter;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiKeyAuthenticationFilter Tests")
class ApiKeyAuthenticationFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private PrintWriter printWriter;

    @InjectMocks
    private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    @BeforeEach
    void setUp() throws IOException {
        ReflectionTestUtils.setField(apiKeyAuthenticationFilter, "validApiKey", "test-api-key");
        ReflectionTestUtils.setField(apiKeyAuthenticationFilter, "apiKeyEnabled", true);
        when(response.getWriter()).thenReturn(printWriter);
    }

    @Test
    @DisplayName("Should allow request with valid API key")
    void testDoFilterInternal_ValidApiKey() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/v1/users");
        when(request.getHeader("X-API-Key")).thenReturn("test-api-key");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        apiKeyAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    @DisplayName("Should reject request without API key")
    void testDoFilterInternal_NoApiKey() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/v1/users");
        when(request.getHeader("X-API-Key")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        apiKeyAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(401);
        verify(printWriter).write(anyString());
    }

    @Test
    @DisplayName("Should reject request with invalid API key")
    void testDoFilterInternal_InvalidApiKey() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/v1/users");
        when(request.getHeader("X-API-Key")).thenReturn("invalid-key");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        apiKeyAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(401);
    }

    @Test
    @DisplayName("Should allow OPTIONS requests")
    void testDoFilterInternal_OptionsRequest() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("OPTIONS");
        when(request.getHeader("Origin")).thenReturn("http://localhost:3000");

        apiKeyAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(200);
        verify(response).setHeader("Access-Control-Allow-Origin", "http://localhost:3000");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Should bypass API key check for public paths")
    void testDoFilterInternal_PublicPath() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/v1/auth/login");

        apiKeyAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(request, never()).getHeader("X-API-Key");
    }

    @Test
    @DisplayName("Should bypass API key check when disabled")
    void testDoFilterInternal_ApiKeyDisabled() throws ServletException, IOException {
        ReflectionTestUtils.setField(apiKeyAuthenticationFilter, "apiKeyEnabled", false);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/v1/users");

        apiKeyAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should handle exception gracefully")
    void testDoFilterInternal_Exception() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/v1/users");
        when(request.getHeader("X-API-Key")).thenThrow(new RuntimeException("Error"));

        apiKeyAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(500);
        verify(printWriter).write(anyString());
    }
}

