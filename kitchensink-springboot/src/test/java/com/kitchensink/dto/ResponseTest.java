package com.kitchensink.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Response DTO Tests")
class ResponseTest {

    @Test
    @DisplayName("Should create success response with data")
    void testSuccessResponse() {
        Response<String> response = Response.success("test-data");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("test-data");
        assertThat(response.getMessage()).isEqualTo("Operation successful");
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should create success response with custom message")
    void testSuccessResponseWithMessage() {
        Response<String> response = Response.success("test-data", "Custom message");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("test-data");
        assertThat(response.getMessage()).isEqualTo("Custom message");
    }

    @Test
    @DisplayName("Should create error response")
    void testErrorResponse() {
        Response<String> response = Response.error("Error message", "ERROR_CODE");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Error message");
        assertThat(response.getError()).isNotNull();
        assertThat(response.getError().getCode()).isEqualTo("ERROR_CODE");
    }

    @Test
    @DisplayName("Should create error response with details")
    void testErrorResponseWithDetails() {
        Response<String> response = Response.error("Error message", "ERROR_CODE", "Details");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError().getDetails()).isEqualTo("Details");
    }

    @Test
    @DisplayName("Should set and get all fields")
    void testGettersAndSetters() {
        Response<String> response = new Response<>();
        LocalDateTime now = LocalDateTime.now();
        Response.ErrorDetails error = new Response.ErrorDetails("CODE", "Message", "Details");

        response.setSuccess(true);
        response.setMessage("Test");
        response.setData("data");
        response.setCorrelationId("corr-1");
        response.setTimestamp(now);
        response.setError(error);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Test");
        assertThat(response.getData()).isEqualTo("data");
        assertThat(response.getCorrelationId()).isEqualTo("corr-1");
        assertThat(response.getTimestamp()).isEqualTo(now);
        assertThat(response.getError()).isEqualTo(error);
    }

    @Test
    @DisplayName("Should create ErrorDetails with constructor")
    void testErrorDetails() {
        Response.ErrorDetails error1 = new Response.ErrorDetails("CODE", "Message");
        assertThat(error1.getCode()).isEqualTo("CODE");
        assertThat(error1.getMessage()).isEqualTo("Message");

        Response.ErrorDetails error2 = new Response.ErrorDetails("CODE", "Message", "Details");
        assertThat(error2.getDetails()).isEqualTo("Details");
    }
}

