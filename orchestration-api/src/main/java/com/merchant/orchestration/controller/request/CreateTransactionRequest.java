package com.merchant.orchestration.controller.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Immutable Record representing incoming request payload for creating a transaction.
 */
@Schema(description = "Payload to initiate a new transaction")
public record CreateTransactionRequest(

        @Schema(description = "Transaction amount as decimal string", example = "250.00")
        @NotBlank(message = "value cannot be blank")
        @Pattern(regexp = "^\\d+(\\.\\d{1,2})?$", message = "value must be a valid positive decimal string (e.g. 250.00)")
        String value,

        @Schema(description = "Transaction description", example = "T-Shirt Black M")
        @NotBlank(message = "description cannot be blank")
        String description,

        @Schema(description = "Payment method (debit_card or credit_card)", example = "credit_card")
        @NotBlank(message = "method cannot be blank")
        @Pattern(regexp = "^(debit_card|credit_card|DEBIT_CARD|CREDIT_CARD)$", message = "method must be debit_card or credit_card")
        String method,

        @Schema(description = "Card number (at least 4 digits)", example = "4532123456783486")
        @NotBlank(message = "cardNumber cannot be blank")
        @Pattern(regexp = "^[0-9\\s]{4,19}$", message = "cardNumber must contain 4 to 19 digits")
        String cardNumber,

        @Schema(description = "Name of the cardholder", example = "Simplenube Store")
        @NotBlank(message = "cardHolderName cannot be blank")
        String cardHolderName,

        @Schema(description = "Card expiration date in MM/YY format", example = "04/28")
        @NotBlank(message = "cardExpirationDate cannot be blank")
        @Pattern(regexp = "^(0[1-9]|1[0-2])/(\\d{2})$", message = "cardExpirationDate must be in MM/YY format")
        String cardExpirationDate,

        @Schema(description = "Card CVV security code (3 or 4 digits)", example = "222")
        @NotBlank(message = "cardCvv cannot be blank")
        @Pattern(regexp = "^\\d{3,4}$", message = "cardCvv must be 3 or 4 digits")
        String cardCvv
) {
}
