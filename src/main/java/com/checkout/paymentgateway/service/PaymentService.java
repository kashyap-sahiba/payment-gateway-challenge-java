package com.checkout.paymentgateway.service;

import com.checkout.paymentgateway.client.BankSimulatorClient;
import com.checkout.paymentgateway.client.BankSimulatorResponse;
import com.checkout.paymentgateway.dto.PaymentRequest;
import com.checkout.paymentgateway.model.Payment;
import com.checkout.paymentgateway.model.PaymentStatus;
import com.checkout.paymentgateway.repository.PaymentsRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private final BankSimulatorClient bankSimulatorClient;
    private final PaymentsRepository paymentsRepository;

    public PaymentService(BankSimulatorClient bankSimulatorClient, PaymentsRepository paymentsRepository) {
        this.bankSimulatorClient = bankSimulatorClient;
        this.paymentsRepository = paymentsRepository;
    }

    public Payment processPayment(PaymentRequest request) {
        String cardNumber = request.getCardNumber();
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .cardNumberLastFour(cardNumber.substring(cardNumber.length() - 4))
                .expiryMonth(request.getExpiryMonth())
                .expiryYear(request.getExpiryYear())
                .currency(request.getCurrency().name())
                .amount(request.getAmount())
                .build();

        BankSimulatorResponse bankResponse = bankSimulatorClient.authorize(request);
        payment.setStatus(bankResponse.isAuthorized() ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED);

        return paymentsRepository.save(payment);
    }

    public Optional<Payment> getPayment(UUID id) {
        return paymentsRepository.findById(id);
    }
}
