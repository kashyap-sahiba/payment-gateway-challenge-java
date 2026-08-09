package com.checkout.paymentgateway.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;

@Value
@Builder
class BankSimulatorRequest {

    @JsonProperty("card_number")
    @ToString.Exclude
    String cardNumber;

    @JsonProperty("expiry_date")
    String expiryDate;

    String currency;

    int amount;

    @ToString.Exclude
    String cvv;
}
