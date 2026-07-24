package com.merchant.orchestration.controller.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.merchant.orchestration.domain.Receivable;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Immutable Record DTO representing the response for a created receivable.
 */
@Schema(description = "Receivable response details")
public record ReceivableResponse(
        @Schema(description = "Unique receivable ID", example = "1")
        @JsonProperty("id") String id,

        @Schema(description = "Associated transaction ID", example = "1")
        @JsonProperty("transaction_id") String transactionId,

        @Schema(description = "Receivable status (paid or waiting_funds)", example = "waiting_funds")
        @JsonProperty("status") String status,

        @Schema(description = "Scheduled payment creation date", example = "2026-08-20")
        @JsonProperty("create_date") String createDate,

        @Schema(description = "Transaction subtotal amount", example = "250.00")
        @JsonProperty("subtotal") String subtotal,

        @Schema(description = "Fee discount deducted amount", example = "10.00")
        @JsonProperty("discount") String discount,

        @Schema(description = "Net receivable total amount", example = "240.00")
        @JsonProperty("total") String total
) {

    public static ReceivableResponse fromDomain(final Receivable receivable) {
        return new ReceivableResponse(
                receivable.id(),
                receivable.transactionId(),
                receivable.status(),
                receivable.createDate(),
                receivable.subtotal(),
                receivable.discount(),
                receivable.total()
        );
    }
}
