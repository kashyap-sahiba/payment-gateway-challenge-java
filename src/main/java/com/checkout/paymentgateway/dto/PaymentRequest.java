package com.checkout.paymentgateway.dto;

import com.checkout.paymentgateway.dto.validation.FutureExpiry;
import com.checkout.paymentgateway.model.Currency;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
@FutureExpiry
public class PaymentRequest {

    @NotNull
    @Pattern(regexp = "\\d{14,19}", message = "cardNumber must be 14-19 numeric characters")
    @ToString.Exclude
    private String cardNumber;

    @NotNull
    @Min(1)
    @Max(12)
    private Integer expiryMonth;

    @NotNull
    private Integer expiryYear;

    @NotNull
    private Currency currency;

    @NotNull
    @Positive
    private Integer amount;

    @NotNull
    @Pattern(regexp = "\\d{3,4}", message = "cvv must be 3-4 numeric characters")
    @ToString.Exclude
    private String cvv;
}
