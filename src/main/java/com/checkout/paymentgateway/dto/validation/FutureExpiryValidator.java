package com.checkout.paymentgateway.dto.validation;

import com.checkout.paymentgateway.dto.PaymentRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.YearMonth;

public class FutureExpiryValidator implements ConstraintValidator<FutureExpiry, PaymentRequest> {

    @Override
    public boolean isValid(PaymentRequest request, ConstraintValidatorContext context) {
        if (request == null || request.getExpiryMonth() == null || request.getExpiryYear() == null) {
            return true;
        }
        int month = request.getExpiryMonth();
        if (month < 1 || month > 12) {
            return true;
        }
        YearMonth expiry = YearMonth.of(request.getExpiryYear(), month);
        return expiry.isAfter(YearMonth.now());
    }
}
