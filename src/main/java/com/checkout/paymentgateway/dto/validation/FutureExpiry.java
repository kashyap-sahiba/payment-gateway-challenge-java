package com.checkout.paymentgateway.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FutureExpiryValidator.class)
public @interface FutureExpiry {
    String message() default "expiryMonth and expiryYear must be in the future";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
