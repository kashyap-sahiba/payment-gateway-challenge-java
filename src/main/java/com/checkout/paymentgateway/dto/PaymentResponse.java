package com.checkout.paymentgateway.dto;

import com.checkout.paymentgateway.model.Payment;
import com.checkout.paymentgateway.model.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PaymentResponse {
    private UUID id;
    private PaymentStatus status;
    private String cardNumberLastFour;
    private int expiryMonth;
    private int expiryYear;
    private String currency;
    private int amount;

    public static PaymentResponse fromPayment(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .status(payment.getStatus())
                .cardNumberLastFour(payment.getCardNumberLastFour())
                .expiryMonth(payment.getExpiryMonth())
                .expiryYear(payment.getExpiryYear())
                .currency(payment.getCurrency())
                .amount(payment.getAmount())
                .build();
    }
}
