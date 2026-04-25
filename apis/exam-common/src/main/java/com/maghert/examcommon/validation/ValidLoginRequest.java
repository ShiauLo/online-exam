package com.maghert.examcommon.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = LoginRequestValidator.class)
public @interface ValidLoginRequest {

    String message() default "invalid login request";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
