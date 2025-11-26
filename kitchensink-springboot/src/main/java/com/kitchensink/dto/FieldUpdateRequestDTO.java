package com.kitchensink.dto;

import com.kitchensink.validation.IndianIsdCode;

public class FieldUpdateRequestDTO {
    
    private String fieldName;
    private String value;
    private String otp;      // Required only for email changes
    
    @IndianIsdCode(message = "ISD code must be +91 for Indian numbers")
    private String isdCode;  // Required for phone number updates
    
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
    
    public String getIsdCode() {
        return isdCode;
    }
    
    public void setIsdCode(String isdCode) {
        this.isdCode = isdCode;
    }
}

