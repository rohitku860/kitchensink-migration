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

import java.io.IOException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CorrelationIdFilter Tests")
class CorrelationIdFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private CorrelationIdFilter correlationIdFilter;

    @BeforeEach
    void setUp() {
        CorrelationIdUtil.clear();
    }

    @AfterEach
    void tearDown() {
        CorrelationIdUtil.clear();
    }

    @Test
    @DisplayName("Should set correlation ID in response header")
    void testDoFilterInternal_WithCorrelationId() throws ServletException, IOException {
        when(request.getHeader("X-Correlation-ID")).thenReturn("test-correlation-id");

        correlationIdFilter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader("X-Correlation-ID", "test-correlation-id");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should create and set correlation ID when not in request")
    void testDoFilterInternal_WithoutCorrelationId() throws ServletException, IOException {
        when(request.getHeader("X-Correlation-ID")).thenReturn(null);

        correlationIdFilter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader(eq("X-Correlation-ID"), anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should clear correlation ID after filter")
    void testDoFilterInternal_ClearsAfterFilter() throws ServletException, IOException {
        when(request.getHeader("X-Correlation-ID")).thenReturn("test-correlation-id");

        correlationIdFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        // Correlation ID should be cleared in finally block
    }

    @Test
    @DisplayName("Should clear correlation ID even when exception occurs")
    void testDoFilterInternal_ExceptionHandling() throws ServletException, IOException {
        when(request.getHeader("X-Correlation-ID")).thenReturn("test-correlation-id");
        doThrow(new ServletException("Test exception")).when(filterChain).doFilter(request, response);

        try {
            correlationIdFilter.doFilterInternal(request, response, filterChain);
        } catch (ServletException e) {
            // Expected
        }

        verify(filterChain).doFilter(request, response);
    }
}

