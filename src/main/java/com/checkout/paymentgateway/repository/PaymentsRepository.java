package com.checkout.paymentgateway.repository;

import com.checkout.paymentgateway.model.Payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentsRepository {
    Payment save(Payment payment);

    Optional<Payment> findById(UUID id);
}
