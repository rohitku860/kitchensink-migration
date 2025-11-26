package com.kitchensink.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CorrelationIdUtil Tests")
class CorrelationIdUtilTest {

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        CorrelationIdUtil.clear();
        MDC.clear();
    }

    @Test
    @DisplayName("Should get correlation ID from request header")
    void testGetOrCreateCorrelationId_FromHeader() {
        String correlationId = "test-correlation-id-123";
        when(request.getHeader("X-Correlation-ID")).thenReturn(correlationId);

        String result = CorrelationIdUtil.getOrCreateCorrelationId(request);

        assertThat(result).isEqualTo(correlationId);
        assertThat(MDC.get("correlationId")).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("Should create new correlation ID when not in header")
    void testGetOrCreateCorrelationId_CreateNew() {
        when(request.getHeader("X-Correlation-ID")).thenReturn(null);

        String result = CorrelationIdUtil.getOrCreateCorrelationId(request);

        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(MDC.get("correlationId")).isEqualTo(result);
    }

    @Test
    @DisplayName("Should create new correlation ID when header is empty")
    void testGetOrCreateCorrelationId_EmptyHeader() {
        when(request.getHeader("X-Correlation-ID")).thenReturn("");

        String result = CorrelationIdUtil.getOrCreateCorrelationId(request);

        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(MDC.get("correlationId")).isEqualTo(result);
    }

    @Test
    @DisplayName("Should get correlation ID from MDC")
    void testGetCorrelationId() {
        String correlationId = "test-correlation-id";
        MDC.put("correlationId", correlationId);

        String result = CorrelationIdUtil.getCorrelationId();

        assertThat(result).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("Should return null when correlation ID not in MDC")
    void testGetCorrelationId_NotInMDC() {
        MDC.clear();

        String result = CorrelationIdUtil.getCorrelationId();

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should clear correlation ID from MDC")
    void testClear() {
        MDC.put("correlationId", "test-correlation-id");

        CorrelationIdUtil.clear();

        assertThat(MDC.get("correlationId")).isNull();
    }
}

