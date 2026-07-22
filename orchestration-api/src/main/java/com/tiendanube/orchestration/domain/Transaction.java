package com.tiendanube.orchestration.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Domain entity representing a merchant transaction with masked card number.
 */
public record Transaction(
        @JsonProperty("id") String id,
        @JsonProperty("value") String value,
        @JsonProperty("description") String description,
        @JsonProperty("method") String methodCode,
        @JsonProperty("cardNumber") String cardNumber,
        @JsonProperty("cardHolderName") String cardHolderName,
        @JsonProperty("cardExpirationDate") String cardExpirationDate,
        @JsonProperty("cardCvv") String cardCvv
) {

    public Transaction(
            final String id,
            final BigDecimal valueAmount,
            final String description,
            final PaymentMethod method,
            final CardDetails cardDetails
    ) {
        this(
                id,
                valueAmount != null ? valueAmount.toPlainString() : "0.00",
                description,
                method != null ? method.getCode() : null,
                cardDetails != null ? cardDetails.getLastFourDigits() : null,
                cardDetails != null ? cardDetails.cardHolderName() : null,
                cardDetails != null ? cardDetails.cardExpirationDate() : null,
                cardDetails != null ? cardDetails.cardCvv() : null
        );
    }

    public PaymentMethod method() {
        return PaymentMethod.fromCode(methodCode);
    }
}
