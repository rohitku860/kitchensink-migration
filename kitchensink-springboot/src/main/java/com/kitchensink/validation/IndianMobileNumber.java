package com.kitchensink.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = IndianMobileNumberValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface IndianMobileNumber {
    String message() default "Phone number must be a valid Indian mobile number (10 digits)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

