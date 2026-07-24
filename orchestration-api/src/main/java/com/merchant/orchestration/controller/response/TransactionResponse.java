package com.merchant.orchestration.controller.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.merchant.orchestration.domain.Transaction;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Immutable Record DTO representing the response for a created/queried transaction.
 */
@Schema(description = "Transaction response details")
public record TransactionResponse(
        @Schema(description = "Unique transaction ID", example = "1")
        @JsonProperty("id") String id,

        @Schema(description = "Transaction value amount", example = "250.00")
        @JsonProperty("value") String value,

        @Schema(description = "Transaction description", example = "T-Shirt")
        @JsonProperty("description") String description,

        @Schema(description = "Payment method code", example = "credit_card")
        @JsonProperty("method") String method,

        @Schema(description = "Masked card number (last 4 digits only)", example = "3486")
        @JsonProperty("cardNumber") String cardNumber,

        @Schema(description = "Name of cardholder", example = "Simplenube Store")
        @JsonProperty("cardHolderName") String cardHolderName,

        @Schema(description = "Card expiration date MM/YY", example = "04/28")
        @JsonProperty("cardExpirationDate") String cardExpirationDate
) {

    public static TransactionResponse fromDomain(final Transaction transaction) {
        return new TransactionResponse(
                transaction.id(),
                transaction.value(),
                transaction.description(),
                transaction.method().getCode(),
                transaction.cardNumber(),
                transaction.cardHolderName(),
                transaction.cardExpirationDate()
        );
    }
}
