package com.kitchensink.config;

import com.kitchensink.model.UserRoleType;
import com.kitchensink.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/v1/auth/**").permitAll()
                // Actuator endpoints - both with and without context path
                .requestMatchers("/actuator/**", "/kitchensink/actuator/**", "**/prometheus/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/swagger-ui.html/**").permitAll()
                .requestMatchers("/api-docs/**", "/v3/api-docs/**", "/v3/api-docs").permitAll()
                .requestMatchers("/swagger-resources/**", "/webjars/**", "/configuration/**").permitAll()
                // Admin endpoints: All require ADMIN role - enforced at URL level (defense in depth)
                // @PreAuthorize on AdminController provides additional method-level enforcement
                .requestMatchers("/v1/admin/**").hasRole(UserRoleType.ADMIN.getName())
                // Profile endpoints: Require authentication at URL level (coarse-grained)
                // Fine-grained access control (admin OR own profile) is enforced by @PreAuthorize on individual methods
                // This allows different methods to have different rules (e.g., revokeUpdateRequest only needs own profile)
                .requestMatchers("/v1/profile/**").authenticated()
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:8080", "http://127.0.0.1:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList(
            "Content-Type", 
            "Authorization", 
            "X-API-Key", 
            "X-Correlation-ID",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        configuration.setExposedHeaders(Arrays.asList("X-Correlation-ID", "Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

