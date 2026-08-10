package com.checkout.paymentgateway.service;

import com.checkout.paymentgateway.client.BankSimulatorClient;
import com.checkout.paymentgateway.client.BankSimulatorResponse;
import com.checkout.paymentgateway.dto.PaymentRequest;
import com.checkout.paymentgateway.model.Payment;
import com.checkout.paymentgateway.model.PaymentStatus;
import com.checkout.paymentgateway.repository.PaymentsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class PaymentService {

    private final BankSimulatorClient bankSimulatorClient;
    private final PaymentsRepository paymentsRepository;

    public PaymentService(BankSimulatorClient bankSimulatorClient, PaymentsRepository paymentsRepository) {
        this.bankSimulatorClient = bankSimulatorClient;
        this.paymentsRepository = paymentsRepository;
    }

    public Payment processPayment(PaymentRequest request) {
        UUID paymentId = UUID.randomUUID();
        String cardNumberLastFour = request.getCardNumber().substring(request.getCardNumber().length() - 4);

        log.info("Payment {} received and validated, cardEnding={}, currency={}, amount={}",
                paymentId, cardNumberLastFour, request.getCurrency(), request.getAmount());

        Payment payment = Payment.builder()
                .id(paymentId)
                .cardNumberLastFour(cardNumberLastFour)
                .expiryMonth(request.getExpiryMonth())
                .expiryYear(request.getExpiryYear())
                .currency(request.getCurrency().name())
                .amount(request.getAmount())
                .build();

        BankSimulatorResponse bankResponse = bankSimulatorClient.authorize(paymentId, request);
        payment.setStatus(bankResponse.isAuthorized() ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED);

        return paymentsRepository.save(payment);
    }

    public Optional<Payment> getPayment(UUID id) {
        return paymentsRepository.findById(id);
    }
}
