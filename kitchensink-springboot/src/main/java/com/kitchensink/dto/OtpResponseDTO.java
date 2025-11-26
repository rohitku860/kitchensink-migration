package com.kitchensink.dto;

public class OtpResponseDTO {
    
    private String message;
    
    public OtpResponseDTO() {
    }
    
    public OtpResponseDTO(String message) {
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}

