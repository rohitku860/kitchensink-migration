package com.kitchensink.filter;

import com.kitchensink.config.RateLimitConfig;
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
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitFilter Tests")
class RateLimitFilterTest {

    @Mock
    private RateLimitConfig rateLimitConfig;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private PrintWriter printWriter;

    @InjectMocks
    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void setUp() throws IOException {
        // Setup will be done per test as needed
    }

    @Test
    @DisplayName("Should allow request when rate limit not exceeded")
    void testDoFilterInternal_Allowed() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/v1/users");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimitConfig.isAllowed("127.0.0.1")).thenReturn(true);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    @DisplayName("Should block request when rate limit exceeded")
    void testDoFilterInternal_RateLimitExceeded() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/v1/users");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimitConfig.isAllowed("127.0.0.1")).thenReturn(false);
        when(response.getWriter()).thenReturn(printWriter);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(response).setContentType("application/json");
        verify(printWriter).write(anyString());
    }

    @Test
    @DisplayName("Should skip rate limit for actuator endpoints")
    void testDoFilterInternal_SkipActuator() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/actuator/health");

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimitConfig, never()).isAllowed(anyString());
    }

    @Test
    @DisplayName("Should skip rate limit for swagger endpoints")
    void testDoFilterInternal_SkipSwagger() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimitConfig, never()).isAllowed(anyString());
    }

    @Test
    @DisplayName("Should skip rate limit for api-docs endpoints")
    void testDoFilterInternal_SkipApiDocs() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api-docs");

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimitConfig, never()).isAllowed(anyString());
    }

    @Test
    @DisplayName("Should get client IP from X-Forwarded-For header")
    void testDoFilterInternal_XForwardedFor() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/v1/users");
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");
        when(rateLimitConfig.isAllowed("192.168.1.1")).thenReturn(true);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(rateLimitConfig).isAllowed("192.168.1.1");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should get client IP from X-Real-IP header")
    void testDoFilterInternal_XRealIP() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/v1/users");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("192.168.1.2");
        when(rateLimitConfig.isAllowed("192.168.1.2")).thenReturn(true);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(rateLimitConfig).isAllowed("192.168.1.2");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should use remote address when no headers present")
    void testDoFilterInternal_RemoteAddr() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/v1/users");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimitConfig.isAllowed("127.0.0.1")).thenReturn(true);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(rateLimitConfig).isAllowed("127.0.0.1");
        verify(filterChain).doFilter(request, response);
    }
}

