package com.kitchensink.filter;

import com.kitchensink.util.CorrelationIdUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequestLoggingFilter Tests")
class RequestLoggingFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private RequestLoggingFilter requestLoggingFilter;

    @BeforeEach
    void setUp() {
        CorrelationIdUtil.clear();
    }

    @AfterEach
    void tearDown() {
        CorrelationIdUtil.clear();
    }

    @Test
    @DisplayName("Should log request and response")
    void testDoFilterInternal_LogsRequest() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/v1/users");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Correlation-ID")).thenReturn("test-correlation-id");
        when(response.getStatus()).thenReturn(200);

        requestLoggingFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(ContentCachingRequestWrapper.class), any(ContentCachingResponseWrapper.class));
    }

    @Test
    @DisplayName("Should skip logging for actuator endpoints")
    void testDoFilterInternal_SkipActuator() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/actuator/health");

        requestLoggingFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should skip logging for swagger endpoints")
    void testDoFilterInternal_SkipSwagger() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");

        requestLoggingFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should get client IP from X-Forwarded-For header")
    void testDoFilterInternal_XForwardedFor() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/v1/users");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");
        when(request.getHeader("X-Correlation-ID")).thenReturn("test-correlation-id");
        when(response.getStatus()).thenReturn(200);

        requestLoggingFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(ContentCachingRequestWrapper.class), any(ContentCachingResponseWrapper.class));
    }

    @Test
    @DisplayName("Should get client IP from X-Real-IP header")
    void testDoFilterInternal_XRealIP() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/v1/users");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("192.168.1.2");
        when(request.getHeader("X-Correlation-ID")).thenReturn("test-correlation-id");
        when(response.getStatus()).thenReturn(200);

        requestLoggingFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(ContentCachingRequestWrapper.class), any(ContentCachingResponseWrapper.class));
    }

    @Test
    @DisplayName("Should use remote address when no headers present")
    void testDoFilterInternal_RemoteAddr() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/v1/users");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Correlation-ID")).thenReturn(null);
        when(response.getStatus()).thenReturn(200);

        requestLoggingFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(ContentCachingRequestWrapper.class), any(ContentCachingResponseWrapper.class));
    }
}

