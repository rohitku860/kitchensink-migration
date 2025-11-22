package com.kitchensink.util;

import java.util.UUID;

import org.slf4j.MDC;

import jakarta.servlet.http.HttpServletRequest;

public class CorrelationIdUtil {
    
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";
    
    public static String getOrCreateCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        return correlationId;
    }
    
    public static String getCorrelationId() {
        return MDC.get(CORRELATION_ID_MDC_KEY);
    }
    
    public static void clear() {
        MDC.remove(CORRELATION_ID_MDC_KEY);
    }
}

