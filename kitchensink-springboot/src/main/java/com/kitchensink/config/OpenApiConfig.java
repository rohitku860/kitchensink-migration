package com.kitchensink.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) Configuration
 * 
 * Provides API documentation at:
 * - Swagger UI: http://localhost:8081/api/swagger-ui.html
 * - API Docs: http://localhost:8081/api/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI kitchensinkOpenAPI() {
        Server localServer = new Server();
        localServer.setUrl("http://localhost:8081/api");
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

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer));
    }
}

