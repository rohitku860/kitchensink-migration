package com.kitchensink.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) Configuration
 * 
 * Provides API documentation at:
 * - Swagger UI: http://localhost:8081/kitchensink/swagger-ui.html
 * - API Docs: http://localhost:8081/kitchensink/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI kitchensinkOpenAPI() {
        Server localServer = new Server();
        localServer.setUrl("http://localhost:8081/kitchensink");
        localServer.setDescription("Local Development Server");

        Contact contact = new Contact();
        contact.setName("Kitchensink API Support");
        contact.setEmail("support@kitchensink.com");

        License license = new License()
                .name("Apache 2.0")
                .url("https://www.apache.org/licenses/LICENSE-2.0.html");

        Info info = new Info()
                .title("Kitchensink User Management API")
                .version("1.0.0")
                .description("REST API for User Registration and Management. " +
                        "Migrated from EJB to Spring Boot with MongoDB backend.")
                .contact(contact)
                .license(license);

        // Security scheme for JWT Bearer token
        SecurityScheme bearerAuth = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT Authentication. Enter your JWT token obtained from /v1/auth/login");

        // Security scheme for API Key
        SecurityScheme apiKeyAuth = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-API-Key")
                .description("API Key for authentication. Required for all endpoints except auth and Swagger UI.");

        Components components = new Components()
                .addSecuritySchemes("bearer-jwt", bearerAuth)
                .addSecuritySchemes("api-key", apiKeyAuth);

        // Note: Security requirements are not added globally here.
        // Individual endpoints can specify their security requirements via @Operation annotation.
        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer))
                .components(components);
    }
}

