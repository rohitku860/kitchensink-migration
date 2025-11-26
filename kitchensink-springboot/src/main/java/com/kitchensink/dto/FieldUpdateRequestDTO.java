package com.kitchensink.dto;

public class FieldUpdateRequestDTO {
    
    private String fieldName;
    private String value;
    private String otp;      // Required only for email changes
    
    public FieldUpdateRequestDTO() {
    }
    
    public FieldUpdateRequestDTO(String fieldName, String value) {
        this.fieldName = fieldName;
        this.value = value;
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    public String getOtp() {
        return otp;
    }
    
    public void setOtp(String otp) {
        this.otp = otp;
    }
}

