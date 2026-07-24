package com.merchant.orchestration.controller;

import com.merchant.orchestration.controller.request.CreateTransactionRequest;
import com.merchant.orchestration.controller.response.OrchestrationResponse;
import com.merchant.orchestration.controller.response.TransactionResponse;
import com.merchant.orchestration.service.OrchestrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller exposing merchant transaction orchestration endpoints.
 */
@Tag(name = "Transactions", description = "Endpoints for orchestrating merchant transactions and receivables")
@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final OrchestrationService orchestrationService;

    public TransactionController(final OrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @Operation(summary = "Create a new merchant transaction", description = "Orchestrates ID allocation, card masking, fee calculation, transaction saving, and receivable creation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transaction and receivable created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrchestrationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or card validation failure",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Numerator conflict or partial persistence failure leading to rollback",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<OrchestrationResponse> createTransaction(
            @Valid @RequestBody final CreateTransactionRequest request
    ) {
        final OrchestrationResponse response = orchestrationService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get transaction by ID", description = "Retrieves a specific merchant transaction by its unique ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Transaction not found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransactionById(@PathVariable final String id) {
        final TransactionResponse response = orchestrationService.getTransactionById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all transactions", description = "Retrieves list of all created merchant transactions.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of transactions retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getAllTransactions() {
        final List<TransactionResponse> responses = orchestrationService.getAllTransactions();
        return ResponseEntity.ok(responses);
    }
}
