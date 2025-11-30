package com.kitchensink.dto;

import com.kitchensink.model.UserRoleType;
import com.kitchensink.validation.AlphaOnly;
import com.kitchensink.validation.IndianIsdCode;
import com.kitchensink.validation.IndianMobileNumber;
import com.kitchensink.validation.ValidDateOfBirth;
import com.kitchensink.validation.ValidEmailDomain;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserRequestDTO {
    
    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    @AlphaOnly(message = "Name must contain only letters and spaces")
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @ValidEmailDomain(message = "Email must have a valid domain")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;
    
    @NotBlank(message = "ISD code is required")
    @IndianIsdCode(message = "ISD code must be +91 for Indian numbers")
    private String isdCode;
    
    @NotBlank(message = "Phone number is required")
    @IndianMobileNumber(message = "Phone number must be a valid Indian mobile number (10 digits starting with 6-9)")
    private String phoneNumber;
    
    @ValidDateOfBirth(message = "Date of birth must be in DD-MM-YYYY format, not be a future date, and not be more than 100 years ago")
    private String dateOfBirth;
    
    @Size(max = 200, message = "Address must not exceed 200 characters")
    private String address;
    
    @Size(max = 50, message = "City must not exceed 50 characters")
    @AlphaOnly(message = "City must contain only letters and spaces")
    private String city;
    
    @Size(max = 50, message = "Country must not exceed 50 characters")
    @AlphaOnly(message = "Country must contain only letters and spaces")
    private String country;
    
    private String role = UserRoleType.USER.getName(); // Default role
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getIsdCode() {
        return isdCode;
    }
    
    public void setIsdCode(String isdCode) {
        this.isdCode = isdCode;
    }
    
    public String getDateOfBirth() {
        return dateOfBirth;
    }
    
    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    public String getCountry() {
        return country;
    }
    
    public void setCountry(String country) {
        this.country = country;
    }
}

