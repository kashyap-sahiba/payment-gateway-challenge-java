package com.checkout.paymentgateway.client;

import com.checkout.paymentgateway.dto.PaymentRequest;
import com.checkout.paymentgateway.exception.BankSimulatorBadRequestException;
import com.checkout.paymentgateway.exception.BankSimulatorUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.util.UUID;

@Slf4j
@Component
public class BankSimulatorClient {

    private final WebClient webClient;

    public BankSimulatorClient(WebClient.Builder webClientBuilder,
                                @Value("${bank-simulator.base-url:http://localhost:8080}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    public BankSimulatorResponse authorize(UUID paymentId, PaymentRequest paymentRequest) {
        BankSimulatorRequest request = BankSimulatorRequest.builder()
                .cardNumber(paymentRequest.getCardNumber())
                .expiryDate("%02d/%04d".formatted(paymentRequest.getExpiryMonth(), paymentRequest.getExpiryYear()))
                .currency(paymentRequest.getCurrency().name())
                .amount(paymentRequest.getAmount())
                .cvv(paymentRequest.getCvv())
                .build();

        log.info("Calling bank simulator for payment {}", paymentId);

        try {
            BankSimulatorResponse bankResponse = webClient.post()
                    .uri("/payments")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(status -> status.value() == 400, errorResponse ->
                            errorResponse.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .doOnNext(body -> log.error("Bank simulator rejected our request for payment {} as malformed, response body: {}", paymentId, body))
                                    .map(body -> new BankSimulatorBadRequestException(
                                            "Bank simulator rejected the request as malformed")))
                    .onStatus(status -> status.value() == 503, errorResponse ->
                            errorResponse.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .doOnNext(body -> log.warn("Bank simulator is unavailable for payment {}, response body: {}", paymentId, body))
                                    .map(body -> new BankSimulatorUnavailableException("Bank simulator is unavailable")))
                    .bodyToMono(BankSimulatorResponse.class)
                    .block();

            log.info("Bank simulator responded for payment {}: authorized={}", paymentId, bankResponse.isAuthorized());
            return bankResponse;
        } catch (WebClientRequestException e) {
            log.warn("Unable to reach bank simulator for payment {}", paymentId, e);
            throw new BankSimulatorUnavailableException("Unable to reach bank simulator", e);
        }
    }
}
