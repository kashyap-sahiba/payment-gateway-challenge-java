package com.checkout.paymentgateway.dto;

import com.checkout.paymentgateway.model.Currency;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
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
    void shouldHaveNoViolationsForValidRequest() {
        PaymentRequest request = validRequestBuilder().build();

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void shouldRejectCardNumberThatIsTooShort() {
        PaymentRequest request = validRequestBuilder().cardNumber("123").build();

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("cardNumber"));
    }

    @Test
    void shouldRejectCardNumberThatIsTooLong() {
        PaymentRequest request = validRequestBuilder().cardNumber("1".repeat(20)).build();

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("cardNumber"));
    }

    @Test
    void shouldRejectNonNumericCardNumber() {
        PaymentRequest request = validRequestBuilder().cardNumber("abcd424242424242").build();

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("cardNumber"));
    }

    @Test
    void shouldRejectMissingCardNumber() {
        PaymentRequest request = validRequestBuilder().cardNumber(null).build();

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("cardNumber"));
    }

    @Test
    void shouldRejectExpiryMonthBelowOne() {
        PaymentRequest request = validRequestBuilder().expiryMonth(0).build();

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("expiryMonth"));
    }

    @Test
    void shouldRejectExpiryMonthAboveTwelve() {
        PaymentRequest request = validRequestBuilder().expiryMonth(13).build();

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("expiryMonth"));
    }

    @Test
    void shouldRejectMissingExpiryYear() {
        PaymentRequest request = validRequestBuilder().expiryYear(null).build();

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("expiryYear"));
    }

    @Test
    void shouldRejectPastExpiryDate() {
        YearMonth past = YearMonth.now().minusMonths(1);
        PaymentRequest request = validRequestBuilder()
                .expiryMonth(past.getMonthValue())
                .expiryYear(past.getYear())
                .build();

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getMessage().contains("future"));
    }

    @Test
    void shouldRejectCurrentMonthExpiryDateAsNotStrictlyInFuture() {
        YearMonth now = YearMonth.now();
        PaymentRequest request = validRequestBuilder()
                .expiryMonth(now.getMonthValue())
                .expiryYear(now.getYear())
                .build();

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getMessage().contains("future"));
    }

    @Test
    void shouldRejectMissingCurrency() {
        PaymentRequest request = validRequestBuilder().currency(null).build();

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("currency"));
    }

    @Test
    void shouldRejectMissingAmount() {
        PaymentRequest request = validRequestBuilder().amount(null).build();

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("amount"));
    }

    @Test
    void shouldRejectZeroOrNegativeAmount() {
        PaymentRequest request = validRequestBuilder().amount(0).build();

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("amount"));
    }

    @Test
    void shouldRejectCvvThatIsTooShort() {
        PaymentRequest request = validRequestBuilder().cvv("12").build();

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("cvv"));
    }

    @Test
    void shouldRejectCvvThatIsTooLong() {
        PaymentRequest request = validRequestBuilder().cvv("12345").build();

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("cvv"));
    }

    @Test
    void shouldRejectNonNumericCvv() {
        PaymentRequest request = validRequestBuilder().cvv("abc").build();

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("cvv"));
    }

    @Test
    void shouldAcceptFourDigitCvv() {
        PaymentRequest request = validRequestBuilder().cvv("1234").build();

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

        assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("cvv"));
    }

    @Test
    void shouldRejectUnsupportedCurrencyDuringJsonDeserialization() {
        ObjectMapper objectMapper = new ObjectMapper();
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

        assertThatThrownBy(() -> objectMapper.readValue(json, PaymentRequest.class))
                .isInstanceOf(InvalidFormatException.class);
    }
}
