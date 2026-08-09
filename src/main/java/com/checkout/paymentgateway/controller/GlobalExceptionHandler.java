package com.checkout.paymentgateway.controller;

import com.checkout.paymentgateway.exception.BankSimulatorBadRequestException;
import com.checkout.paymentgateway.exception.BankSimulatorUnavailableException;
import com.checkout.paymentgateway.exception.PaymentNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationFailure(MethodArgumentNotValidException ex) {
        String reason = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (reason.isBlank()) {
            reason = ex.getBindingResult().getGlobalErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.joining("; "));
        }

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Rejected");
        problem.setDetail("Payment request rejected: " + reason);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMalformedRequestBody(HttpMessageNotReadableException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Rejected");
        problem.setDetail("Payment request rejected: request body is malformed or contains an unsupported value");
        return problem;
    }

    @ExceptionHandler(BankSimulatorBadRequestException.class)
    public ProblemDetail handleBankSimulatorBadRequest(BankSimulatorBadRequestException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
        problem.setTitle("Bank Integration Error");
        problem.setDetail("The bank rejected this payment as malformed. This indicates a gateway integration bug, not a problem with your request.");
        return problem;
    }

    @ExceptionHandler(BankSimulatorUnavailableException.class)
    public ProblemDetail handleBankSimulatorUnavailable(BankSimulatorUnavailableException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
        problem.setTitle("Bank Unavailable");
        problem.setDetail("The bank could not be reached to process this payment. Please try again later.");
        return problem;
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ProblemDetail handlePaymentNotFound(PaymentNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Payment Not Found");
        problem.setDetail(ex.getMessage());
        return problem;
    }
}
