package com.tiendanube.orchestration.client;

import com.tiendanube.orchestration.domain.AuditAlertCode;
import com.tiendanube.orchestration.domain.Receivable;
import com.tiendanube.orchestration.domain.Transaction;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * HTTP Client executing I/O operations against json-server persistence service,
 * protected by Resilience4j CircuitBreaker for network fault tolerance.
 * Includes resilient compensating transaction rollbacks with retries and critical audit alerts.
 */
@Slf4j
@Component
public class PersistenceClient {

    private final RestClient restClient;
    private final String persistenceUrl;

    public PersistenceClient(
            final RestClient restClient,
            @Value("${services.persistence.url:http://localhost:8080}") final String persistenceUrl
    ) {
        this.restClient = restClient;
        this.persistenceUrl = persistenceUrl;
    }

    /**
     * Persists a transaction entity into json-server, protected by Resilience4j CircuitBreaker.
     *
     * @param transaction entity to persist
     * @return persisted transaction response
     */
    @CircuitBreaker(name = "persistenceService")
    public Transaction saveTransaction(final Transaction transaction) {
        log.debug("Persisting transaction ID: {}", transaction.id());
        final long start = System.currentTimeMillis();
        final Transaction response = restClient.post()
                .uri(persistenceUrl + "/transactions")
                .body(transaction)
                .retrieve()
                .body(Transaction.class);
        log.debug("Transaction ID: {} persisted successfully in {} ms", transaction.id(), System.currentTimeMillis() - start);
        return response;
    }

    /**
     * Persists a receivable entity into json-server, protected by Resilience4j CircuitBreaker.
     *
     * @param receivable entity to persist
     * @return persisted receivable response
     */
    @CircuitBreaker(name = "persistenceService")
    public Receivable saveReceivable(final Receivable receivable) {
        log.debug("Persisting receivable ID: {} for transaction ID: {}", receivable.id(), receivable.transactionId());
        final long start = System.currentTimeMillis();
        final Receivable response = restClient.post()
                .uri(persistenceUrl + "/receivables")
                .body(receivable)
                .retrieve()
                .body(Receivable.class);
        log.debug("Receivable ID: {} persisted successfully in {} ms", receivable.id(), System.currentTimeMillis() - start);
        return response;
    }

    /**
     * Performs a resilient compensating transaction DELETE to remove a transaction by ID.
     * Applies exponential backoff retries and logs CRITICAL_AUDIT_ALERT if all retries fail.
     *
     * @param id transaction ID to delete
     */
    @CircuitBreaker(name = "persistenceService")
    public void deleteTransaction(final String id) {
        log.warn("Initiating compensating DELETE for transaction ID: {}", id);
        final int maxAttempts = 3;
        final int initialDelayMs = 50;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                restClient.delete()
                        .uri(persistenceUrl + "/transactions/{id}", id)
                        .retrieve()
                        .toBodilessEntity();
                log.info("Compensating DELETE succeeded for transaction ID: {} (attempt {})", id, attempt);
                return;
            } catch (Exception e) {
                log.warn("Compensating DELETE attempt {}/{} failed for transaction ID: {}: {}",
                        attempt, maxAttempts, id, e.getMessage());
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep((long) initialDelayMs * (1 << (attempt - 1)));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        log.error("{}: Failed compensating DELETE for transaction ID: {} after {} attempts. Manual DB reconciliation required!",
                AuditAlertCode.CRITICAL_AUDIT_ALERT, id, maxAttempts);
    }

    /**
     * Fetches a transaction by ID, protected by Resilience4j CircuitBreaker.
     *
     * @param id transaction ID
     * @return Optional containing transaction if found
     */
    @CircuitBreaker(name = "persistenceService")
    public Optional<Transaction> getTransactionById(final String id) {
        try {
            final Transaction transaction = restClient.get()
                    .uri(persistenceUrl + "/transactions/{id}", id)
                    .retrieve()
                    .body(Transaction.class);
            return Optional.ofNullable(transaction);
        } catch (Exception e) {
            log.debug("Transaction not found for ID: {}: {}", id, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Fetches list of all transactions, protected by Resilience4j CircuitBreaker.
     *
     * @return list of transactions
     */
    @CircuitBreaker(name = "persistenceService")
    public List<Transaction> getAllTransactions() {
        try {
            final List<Transaction> transactions = restClient.get()
                    .uri(persistenceUrl + "/transactions")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return transactions != null ? transactions : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to retrieve all transactions from {}", persistenceUrl, e);
            return Collections.emptyList();
        }
    }
}
