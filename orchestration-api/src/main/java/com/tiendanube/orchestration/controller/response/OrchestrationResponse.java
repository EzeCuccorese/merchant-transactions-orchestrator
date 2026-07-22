package com.tiendanube.orchestration.controller.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Immutable Record DTO representing the orchestrated creation response containing transaction and receivable.
 */
@Schema(description = "Orchestrated creation response containing transaction and generated receivable")
public record OrchestrationResponse(
        @Schema(description = "Created transaction details")
        @JsonProperty("transaction") TransactionResponse transaction,

        @Schema(description = "Created receivable details")
        @JsonProperty("receivable") ReceivableResponse receivable
) {
}
