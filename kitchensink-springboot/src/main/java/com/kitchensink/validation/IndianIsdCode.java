package com.kitchensink.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = IndianIsdCodeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface IndianIsdCode {
    String message() default "ISD code must be +91 for Indian numbers";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

