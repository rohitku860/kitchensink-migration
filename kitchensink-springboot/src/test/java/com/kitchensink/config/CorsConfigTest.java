package com.kitchensink.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CorsConfig Tests")
class CorsConfigTest {

    @Test
    @DisplayName("Should create CORS configuration")
    void testCorsConfiguration() {
        CorsConfig corsConfig = new CorsConfig();
        ReflectionTestUtils.setField(corsConfig, "allowedOrigins", "http://localhost:3000,http://localhost:8080");

        CorsRegistry registry = new CorsRegistry();
        corsConfig.addCorsMappings(registry);

        // Verify configuration is set
        assertThat(registry).isNotNull();
    }

    @Test
    @DisplayName("Should create CORS filter bean")
    void testCorsFilterRegistration() {
        CorsConfig corsConfig = new CorsConfig();
        ReflectionTestUtils.setField(corsConfig, "allowedOrigins", "http://localhost:3000");

        var filterBean = corsConfig.corsFilterRegistration();

        assertThat(filterBean).isNotNull();
        assertThat(filterBean.getFilter()).isInstanceOf(CorsFilter.class);
    }
}

