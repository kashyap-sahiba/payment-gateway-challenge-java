package com.checkout.paymentgateway.service;

import com.checkout.paymentgateway.client.BankSimulatorClient;
import com.checkout.paymentgateway.client.BankSimulatorResponse;
import com.checkout.paymentgateway.dto.PaymentRequest;
import com.checkout.paymentgateway.exception.BankSimulatorUnavailableException;
import com.checkout.paymentgateway.model.Currency;
import com.checkout.paymentgateway.model.Payment;
import com.checkout.paymentgateway.model.PaymentStatus;
import com.checkout.paymentgateway.repository.PaymentsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private BankSimulatorClient bankSimulatorClient;

    @Mock
    private PaymentsRepository paymentsRepository;

    private PaymentService paymentService;

    private PaymentRequest.PaymentRequestBuilder validRequestBuilder() {
        return PaymentRequest.builder()
                .cardNumber("4242424242424242")
                .expiryMonth(12)
                .expiryYear(2099)
                .currency(Currency.GBP)
                .amount(1000)
                .cvv("123");
    }

    @Test
    void shouldMarkPaymentAsAuthorizedWhenBankAuthorizes() {
        paymentService = new PaymentService(bankSimulatorClient, paymentsRepository);
        PaymentRequest request = validRequestBuilder().build();
        when(bankSimulatorClient.authorize(any(UUID.class), eq(request)))
                .thenReturn(BankSimulatorResponse.builder().authorized(true).authorizationCode("AUTH123").build());
        when(paymentsRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = paymentService.processPayment(request);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(result.getCardNumberLastFour()).isEqualTo("4242");
        assertThat(result.getExpiryMonth()).isEqualTo(12);
        assertThat(result.getExpiryYear()).isEqualTo(2099);
        assertThat(result.getCurrency()).isEqualTo("GBP");
        assertThat(result.getAmount()).isEqualTo(1000);
        assertThat(result.getId()).isNotNull();

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentsRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
    }

    @Test
    void shouldMarkPaymentAsDeclinedWhenBankDeclines() {
        paymentService = new PaymentService(bankSimulatorClient, paymentsRepository);
        PaymentRequest request = validRequestBuilder().build();
        when(bankSimulatorClient.authorize(any(UUID.class), eq(request)))
                .thenReturn(BankSimulatorResponse.builder().authorized(false).authorizationCode(null).build());
        when(paymentsRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = paymentService.processPayment(request);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.DECLINED);
        verify(paymentsRepository).save(any(Payment.class));
    }

    @Test
    void shouldPropagateExceptionAndNotSaveWhenBankSimulatorUnavailable() {
        paymentService = new PaymentService(bankSimulatorClient, paymentsRepository);
        PaymentRequest request = validRequestBuilder().build();
        when(bankSimulatorClient.authorize(any(UUID.class), eq(request)))
                .thenThrow(new BankSimulatorUnavailableException("Bank simulator is unavailable"));

        assertThatThrownBy(() -> paymentService.processPayment(request))
                .isInstanceOf(BankSimulatorUnavailableException.class);

        verify(paymentsRepository, never()).save(any(Payment.class));
    }

    @Test
    void shouldReturnPaymentFromRepositoryWhenFound() {
        paymentService = new PaymentService(bankSimulatorClient, paymentsRepository);
        UUID id = UUID.randomUUID();
        Payment payment = Payment.builder().id(id).status(PaymentStatus.AUTHORIZED).build();
        when(paymentsRepository.findById(id)).thenReturn(Optional.of(payment));

        Optional<Payment> result = paymentService.getPayment(id);

        assertThat(result).contains(payment);
    }

    @Test
    void shouldReturnEmptyWhenPaymentNotFound() {
        paymentService = new PaymentService(bankSimulatorClient, paymentsRepository);
        UUID id = UUID.randomUUID();
        when(paymentsRepository.findById(id)).thenReturn(Optional.empty());

        Optional<Payment> result = paymentService.getPayment(id);

        assertThat(result).isEmpty();
    }
}
