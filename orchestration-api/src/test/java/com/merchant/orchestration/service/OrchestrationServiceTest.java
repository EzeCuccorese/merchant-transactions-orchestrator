package com.merchant.orchestration.service;

import com.merchant.orchestration.client.NumeratorClient;
import com.merchant.orchestration.client.PersistenceClient;
import com.merchant.orchestration.controller.request.CreateTransactionRequest;
import com.merchant.orchestration.controller.response.OrchestrationResponse;
import com.merchant.orchestration.controller.response.TransactionResponse;
import com.merchant.orchestration.domain.CardDetails;
import com.merchant.orchestration.domain.PaymentMethod;
import com.merchant.orchestration.domain.Receivable;
import com.merchant.orchestration.domain.Transaction;
import com.merchant.orchestration.domain.exception.OrchestrationRollbackException;
import com.merchant.orchestration.domain.exception.TransactionNotFoundException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrchestrationServiceTest {

    @Mock
    private NumeratorClient numeratorClient;

    @Mock
    private PersistenceClient persistenceClient;

    private MeterRegistry meterRegistry;
    private OrchestrationService orchestrationService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        orchestrationService = new OrchestrationService(numeratorClient, persistenceClient, meterRegistry);
    }

    @Test
    @DisplayName("Should successfully orchestrate transaction and receivable creation with single CAS pair reservation")
    void shouldOrchestrateTransactionCreationSuccessfully() {
        final CreateTransactionRequest request = new CreateTransactionRequest(
                "340.50",
                "T-Shirt",
                "debit_card",
                "4532123456783486",
                "Fonsi Julian",
                "04/28",
                "290"
        );

        when(numeratorClient.generateUniqueIdPair())
                .thenReturn(new NumeratorClient.IdPair("100", "200"));

        when(persistenceClient.saveTransaction(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(persistenceClient.saveReceivable(any(Receivable.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final OrchestrationResponse response = orchestrationService.createTransaction(request);

        assertThat(response.transaction().id()).isEqualTo("100");
        assertThat(response.transaction().cardNumber()).isEqualTo("3486");
        assertThat(response.receivable().id()).isEqualTo("200");
        assertThat(response.receivable().transactionId()).isEqualTo("100");

        verify(persistenceClient, never()).deleteTransaction(any());
        assertThat(meterRegistry.find("transactions.created.total").counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should execute compensating DELETE transaction when receivable creation fails")
    void shouldTriggerCompensatingDeleteWhenReceivableFails() {
        final CreateTransactionRequest request = new CreateTransactionRequest(
                "340.50",
                "T-Shirt",
                "debit_card",
                "4532123456783486",
                "Fonsi Julian",
                "04/28",
                "290"
        );

        when(numeratorClient.generateUniqueIdPair())
                .thenReturn(new NumeratorClient.IdPair("100", "200"));

        when(persistenceClient.saveTransaction(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(persistenceClient.saveReceivable(any(Receivable.class)))
                .thenThrow(new RuntimeException("json-server network error"));

        assertThatThrownBy(() -> orchestrationService.createTransaction(request))
                .isInstanceOf(OrchestrationRollbackException.class)
                .hasMessageContaining("Receivable persistence failed for transaction ID: 100");

        verify(persistenceClient).deleteTransaction("100");
        assertThat(meterRegistry.find("orchestration.rollback.total").counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should return TransactionResponse when querying transaction by ID")
    void shouldGetTransactionByIdSuccessfully() {
        final CardDetails cardDetails = new CardDetails("4532123456783486", "Fonsi Julian", "04/28", "290");
        final Transaction transaction = new Transaction("100", new BigDecimal("340.50"), "T-Shirt", PaymentMethod.DEBIT_CARD, cardDetails);

        when(persistenceClient.getTransactionById("100")).thenReturn(Optional.of(transaction));

        final TransactionResponse response = orchestrationService.getTransactionById("100");
        assertThat(response.id()).isEqualTo("100");
    }

    @Test
    @DisplayName("Should throw TransactionNotFoundException when transaction ID does not exist")
    void shouldThrowExceptionWhenTransactionNotFound() {
        when(persistenceClient.getTransactionById("999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orchestrationService.getTransactionById("999"))
                .isInstanceOf(TransactionNotFoundException.class)
                .hasMessageContaining("Transaction not found with ID: 999");
    }

    @Test
    @DisplayName("Should return all transactions using Java 21 Sequenced Collections getFirst()")
    void shouldGetAllTransactions() {
        final CardDetails cardDetails = new CardDetails("4532123456783486", "Fonsi Julian", "04/28", "290");
        final Transaction transaction = new Transaction("100", new BigDecimal("340.50"), "T-Shirt", PaymentMethod.DEBIT_CARD, cardDetails);

        when(persistenceClient.getAllTransactions()).thenReturn(List.of(transaction));

        final List<TransactionResponse> responses = orchestrationService.getAllTransactions();
        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().id()).isEqualTo("100");
    }
}
