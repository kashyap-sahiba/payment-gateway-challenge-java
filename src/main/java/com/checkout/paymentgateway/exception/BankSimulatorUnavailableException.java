package com.checkout.paymentgateway.exception;

public class BankSimulatorUnavailableException extends RuntimeException {

    public BankSimulatorUnavailableException(String message) {
        super(message);
    }

    public BankSimulatorUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
