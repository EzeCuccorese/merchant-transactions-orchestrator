package com.tiendanube.orchestration.service;

import com.tiendanube.orchestration.client.NumeratorClient;
import com.tiendanube.orchestration.client.PersistenceClient;
import com.tiendanube.orchestration.controller.request.CreateTransactionRequest;
import com.tiendanube.orchestration.controller.response.OrchestrationResponse;
import com.tiendanube.orchestration.controller.response.ReceivableResponse;
import com.tiendanube.orchestration.controller.response.TransactionResponse;
import com.tiendanube.orchestration.domain.CardDetails;
import com.tiendanube.orchestration.domain.PaymentMethod;
import com.tiendanube.orchestration.domain.Receivable;
import com.tiendanube.orchestration.domain.Transaction;
import com.tiendanube.orchestration.domain.exception.OrchestrationRollbackException;
import com.tiendanube.orchestration.domain.exception.TransactionNotFoundException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Core Orchestration Service coordinating unique ID generation, domain calculations,
 * transaction persistence, receivable creation, and compensating rollbacks.
 *
 * Features:
 * - Single-step CAS Pair Reservation via NumeratorClient.generateUniqueIdPair().
 * - Full Micrometer Business Metrics instrumentation.
 * - Clean, non-repetitive milestone logging.
 */
@Slf4j
@Service
public class OrchestrationService {

    private final NumeratorClient numeratorClient;
    private final PersistenceClient persistenceClient;
    private final MeterRegistry meterRegistry;

    public OrchestrationService(
            final NumeratorClient numeratorClient,
            final PersistenceClient persistenceClient,
            final MeterRegistry meterRegistry
    ) {
        this.numeratorClient = numeratorClient;
        this.persistenceClient = persistenceClient;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Orchestrates the complete transaction creation workflow.
     *
     * @param request transaction creation request payload
     * @return OrchestrationResponse containing transaction and receivable details
     * @throws OrchestrationRollbackException if receivable persistence fails after saving transaction
     */
    public OrchestrationResponse createTransaction(final CreateTransactionRequest request) {
        final long start = System.currentTimeMillis();
        final PaymentMethod paymentMethod = PaymentMethod.fromCode(request.method());
        final BigDecimal transactionValue = new BigDecimal(request.value());

        log.debug("Initiating transaction orchestration for method: {}, value: {}", paymentMethod.getCode(), transactionValue);

        // 1. Single-step CAS Pair Reservation for Transaction ID and Receivable ID
        final NumeratorClient.IdPair idPair = numeratorClient.generateUniqueIdPair();
        final String transactionId = idPair.firstId();
        final String receivableId = idPair.secondId();

        log.debug("Assigned CAS IDs - TxID: {}, ReceivableID: {}", transactionId, receivableId);

        // 2. Value Object & Entity Creation (PCI-DSS masking)
        final CardDetails cardDetails = new CardDetails(
                request.cardNumber(),
                request.cardHolderName(),
                request.cardExpirationDate(),
                request.cardCvv()
        );

        final Transaction transactionToSave = new Transaction(
                transactionId,
                transactionValue,
                request.description(),
                paymentMethod,
                cardDetails
        );

        // 3. Persist Transaction
        final Transaction savedTransaction = persistenceClient.saveTransaction(transactionToSave);

        // 4. Domain Financial Calculations for Receivable
        final BigDecimal discount = paymentMethod.calculateFee(transactionValue);
        final BigDecimal total = paymentMethod.calculateNetTotal(transactionValue);
        final LocalDate createDate = paymentMethod.calculatePaymentDate(LocalDate.now());

        final Receivable receivableToSave = new Receivable(
                receivableId,
                transactionId,
                paymentMethod.getStatus(),
                createDate.toString(),
                transactionValue.setScale(2, java.math.RoundingMode.HALF_UP).toString(),
                discount.toString(),
                total.toString()
        );

        // 5. Persist Receivable with Compensating DELETE Rollback on failure
        final Receivable savedReceivable;
        try {
            savedReceivable = persistenceClient.saveReceivable(receivableToSave);
        } catch (Exception e) {
            log.error("Receivable persistence failed for TxID: {}. Triggering compensating DELETE rollback", transactionId, e);
            recordRollbackMetric();
            persistenceClient.deleteTransaction(transactionId);
            throw new OrchestrationRollbackException("Receivable persistence failed for transaction ID: " + transactionId, e);
        }

        final long duration = System.currentTimeMillis() - start;
        // Consolidated Milestone INFO Log
        log.info("Transaction [txId={}, receivableId={}] created successfully [method={}, status={}, value={}, netTotal={}] in {} ms",
                transactionId, receivableId, paymentMethod.getCode(), savedReceivable.status(), transactionValue, total, duration);

        // 6. Record Business Metrics
        recordBusinessMetrics(paymentMethod, savedReceivable.status(), transactionValue, duration);

        // 7. Map to DTOs via static factory methods
        return new OrchestrationResponse(
                TransactionResponse.fromDomain(savedTransaction),
                ReceivableResponse.fromDomain(savedReceivable)
        );
    }

    /**
     * Queries a transaction by ID.
     *
     * @param id transaction ID
     * @return TransactionResponse DTO
     */
    public TransactionResponse getTransactionById(final String id) {
        log.debug("Fetching transaction by ID: {}", id);
        return persistenceClient.getTransactionById(id)
                .map(TransactionResponse::fromDomain)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with ID: " + id));
    }

    /**
     * Queries all transactions.
     *
     * @return List of TransactionResponse DTOs
     */
    public List<TransactionResponse> getAllTransactions() {
        log.debug("Fetching all transactions");
        return persistenceClient.getAllTransactions().stream()
                .map(TransactionResponse::fromDomain)
                .toList();
    }

    private void recordBusinessMetrics(
            final PaymentMethod method,
            final String status,
            final BigDecimal amount,
            final long durationMs
    ) {
        if (meterRegistry != null) {
            Counter.builder("transactions.created.total")
                    .tag("payment_method", method.getCode())
                    .tag("status", status)
                    .description("Total number of created merchant transactions")
                    .register(meterRegistry)
                    .increment();

            Counter.builder("transactions.amount.total")
                    .tag("payment_method", method.getCode())
                    .description("Total monetary volume of created merchant transactions")
                    .register(meterRegistry)
                    .increment(amount.doubleValue());

            Timer.builder("orchestration.creation.latency")
                    .description("End-to-end latency of transaction creation orchestration")
                    .register(meterRegistry)
                    .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }

    private void recordRollbackMetric() {
        if (meterRegistry != null) {
            Counter.builder("orchestration.rollback.total")
                    .description("Total number of compensating DELETE rollbacks executed")
                    .register(meterRegistry)
                    .increment();
        }
    }
}
