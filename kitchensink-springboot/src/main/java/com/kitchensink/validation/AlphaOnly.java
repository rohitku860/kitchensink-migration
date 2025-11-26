package com.kitchensink.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = AlphaOnlyValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface AlphaOnly {
    String message() default "Field must contain only letters and spaces";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

