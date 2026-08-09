package com.checkout.paymentgateway.controller;

import com.checkout.paymentgateway.dto.PaymentRequest;
import com.checkout.paymentgateway.dto.PaymentResponse;
import com.checkout.paymentgateway.model.Currency;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.time.YearMonth;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentsControllerIntegrationTest {

    private static final MockWebServer bankSimulator = new MockWebServer();

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeAll
    static void startMockWebServer() throws IOException {
        bankSimulator.start();
    }

    @AfterAll
    static void stopMockWebServer() throws IOException {
        bankSimulator.shutdown();
    }

    @AfterEach
    void drainRequests() throws InterruptedException {
        while (bankSimulator.takeRequest(100, TimeUnit.MILLISECONDS) != null) {
            // drain any requests left over from this test so they don't leak into the next one
        }
    }

    @DynamicPropertySource
    static void bankSimulatorProperties(DynamicPropertyRegistry registry) {
        registry.add("bank-simulator.base-url", () -> bankSimulator.url("/").toString());
    }

    private PaymentRequest.PaymentRequestBuilder validRequestBuilder() {
        YearMonth future = YearMonth.now().plusMonths(6);
        return PaymentRequest.builder()
                .cardNumber("4242424242424242")
                .expiryMonth(future.getMonthValue())
                .expiryYear(future.getYear())
                .currency(Currency.GBP)
                .amount(1000)
                .cvv("123");
    }

    @Test
    void shouldReturnCreatedWithAuthorizedStatusWhenBankAuthorizesPayment() {
        bankSimulator.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"authorized\": true, \"authorization_code\": \"AUTH123\"}")
                .addHeader("Content-Type", "application/json"));

        ResponseEntity<PaymentResponse> response = restTemplate.postForEntity(
                "/payments", validRequestBuilder().build(), PaymentResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus().name()).isEqualTo("AUTHORIZED");
        assertThat(response.getBody().getCardNumberLastFour()).isEqualTo("4242");
    }

    @Test
    void shouldReturnCreatedWithDeclinedStatusWhenBankDeclinesPayment() {
        bankSimulator.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"authorized\": false, \"authorization_code\": null}")
                .addHeader("Content-Type", "application/json"));

        ResponseEntity<PaymentResponse> response = restTemplate.postForEntity(
                "/payments", validRequestBuilder().build(), PaymentResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus().name()).isEqualTo("DECLINED");
    }

    @Test
    void shouldReturnBadGatewayWhenBankSimulatorIsUnavailable() {
        bankSimulator.enqueue(new MockResponse().setResponseCode(503));

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/payments", validRequestBuilder().build(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void shouldReturnBadRequestWhenPaymentRequestFailsValidation() {
        long requestsBefore = bankSimulator.getRequestCount();
        PaymentRequest invalidRequest = validRequestBuilder().cardNumber("123").build();

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/payments", invalidRequest, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(bankSimulator.getRequestCount()).isEqualTo(requestsBefore);
    }

    @Test
    void shouldReturnPaymentWhenFetchingByIdAfterCreation() {
        bankSimulator.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"authorized\": true, \"authorization_code\": \"AUTH123\"}")
                .addHeader("Content-Type", "application/json"));

        ResponseEntity<PaymentResponse> createResponse = restTemplate.postForEntity(
                "/payments", validRequestBuilder().build(), PaymentResponse.class);
        var createdPaymentId = createResponse.getBody().getId();

        ResponseEntity<PaymentResponse> getResponse = restTemplate.getForEntity(
                "/payments/" + createdPaymentId, PaymentResponse.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().getId()).isEqualTo(createdPaymentId);
    }

    @Test
    void shouldReturnNotFoundWhenPaymentDoesNotExist() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/payments/" + java.util.UUID.randomUUID(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturnRejectedBadRequestWhenCurrencyIsUnsupported() {
        long requestsBefore = bankSimulator.getRequestCount();
        String json = """
                {
                  "cardNumber": "4242424242424242",
                  "expiryMonth": 12,
                  "expiryYear": 2099,
                  "currency": "JPY",
                  "amount": 1000,
                  "cvv": "123"
                }
                """;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/payments", new HttpEntity<>(json, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Rejected");
        assertThat(bankSimulator.getRequestCount()).isEqualTo(requestsBefore);
    }

    @Test
    void shouldReturnRejectedBadRequestWhenBodyIsMalformedJson() {
        long requestsBefore = bankSimulator.getRequestCount();
        String malformedJson = "{ \"cardNumber\": \"4242424242424242\", ";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/payments", new HttpEntity<>(malformedJson, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Rejected");
        assertThat(bankSimulator.getRequestCount()).isEqualTo(requestsBefore);
    }
}
