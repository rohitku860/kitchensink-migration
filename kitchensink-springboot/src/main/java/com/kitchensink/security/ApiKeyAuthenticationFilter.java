package com.kitchensink.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@Order(2) // Run after CORS filter
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);
    private static final String API_KEY_HEADER = "X-API-Key";
    
    @Value("${app.api.key:}")
    private String validApiKey;
    
    @Value("${app.api.enabled:true}")
    private boolean apiKeyEnabled;
    
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/actuator",
            "/swagger",
            "/swagger-ui",
            "/api-docs",
            "/v3/api-docs",
            "/swagger-ui.html",
            "/swagger-ui/index.html",
            "/v1/auth",
            "/kitchensink/actuator",
            "/kitchensink/swagger",
            "/kitchensink/swagger-ui",
            "/kitchensink/api-docs",
            "/kitchensink/v3/api-docs",
            "/kitchensink/swagger-ui.html",
            "/kitchensink/swagger-ui/index.html",
            "/kitchensink/v1/auth"
    );
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            // Allow OPTIONS requests (CORS preflight) to pass through with proper headers
            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
                response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
                response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-API-Key, X-Correlation-ID, Accept, Origin, Access-Control-Request-Method, Access-Control-Request-Headers");
                response.setHeader("Access-Control-Allow-Credentials", "true");
                response.setHeader("Access-Control-Max-Age", "3600");
                response.setStatus(HttpServletResponse.SC_OK);
                return;
            }
            
            if (!apiKeyEnabled) {
                filterChain.doFilter(request, response);
                return;
            }
            
            String requestUri = request.getRequestURI();
            if (isPublicPath(requestUri)) {
                logger.debug("Bypassing API key check for public path: {}", requestUri);
                filterChain.doFilter(request, response);
                return;
            }
            
            String apiKey = request.getHeader(API_KEY_HEADER);
            
            if (apiKey == null || apiKey.isEmpty()) {
                logger.warn("API request without API key from IP: {}", getClientIpAddress(request));
                sendUnauthorizedResponse(response, "API key is required. Please provide X-API-Key header.");
                return;
            }
            
            if (!isValidApiKey(apiKey)) {
                logger.warn("Invalid API key attempt from IP: {}", getClientIpAddress(request));
                sendUnauthorizedResponse(response, "Invalid API key.");
                return;
            }
            
            logger.debug("Valid API key authenticated for request: {}", requestUri);
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            logger.error("Error in ApiKeyAuthenticationFilter for URI: {}", request.getRequestURI(), e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setContentType("application/json");
            response.getWriter().write(String.format("{\"error\":\"Internal server error\",\"code\":\"INTERNAL_ERROR\"}"));
        }
    }
    
    private boolean isPublicPath(String requestUri) {
        if (requestUri == null || requestUri.isEmpty()) {
            return false;
        }
       
        // Check if path starts with any public path
        boolean isPublic = PUBLIC_PATHS.stream().anyMatch(path -> requestUri.startsWith(path)) ||
                          requestUri.startsWith("/kitchensink/v1/auth") ||
                          requestUri.startsWith("/v1/auth");
        
        if (logger.isDebugEnabled()) {
            logger.debug("Checking public path - URI: {}, isPublic: {}", requestUri, isPublic);
        }
        return isPublic;
    }
    
    private boolean isValidApiKey(String apiKey) {
        if (validApiKey == null || validApiKey.isEmpty()) {
            logger.error("API key not configured in application.properties");
            return false;
        }
        return validApiKey.equals(apiKey);
    }
    
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"error\":\"%s\",\"code\":\"UNAUTHORIZED\"}", message));
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}

