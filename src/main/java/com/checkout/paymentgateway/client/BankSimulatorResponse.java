package com.checkout.paymentgateway.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class BankSimulatorResponse {

    boolean authorized;

    @JsonProperty("authorization_code")
    String authorizationCode;
}
