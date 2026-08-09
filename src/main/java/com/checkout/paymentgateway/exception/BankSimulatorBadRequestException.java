package com.checkout.paymentgateway.exception;

public class BankSimulatorBadRequestException extends RuntimeException {

    public BankSimulatorBadRequestException(String message) {
        super(message);
    }
}
