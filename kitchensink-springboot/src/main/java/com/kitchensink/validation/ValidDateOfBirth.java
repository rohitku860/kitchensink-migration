package com.kitchensink.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidDateOfBirthValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDateOfBirth {
    String message() default "Date of birth must be in DD-MM-YYYY format, not be a future date, and not be more than 100 years ago";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

