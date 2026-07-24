package com.merchant.orchestration.client;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.merchant.orchestration.domain.CardDetails;
import com.merchant.orchestration.domain.PaymentMethod;
import com.merchant.orchestration.domain.Receivable;
import com.merchant.orchestration.domain.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@WireMockTest
class PersistenceClientTest {

    private PersistenceClient persistenceClient;

    @BeforeEach
    void setUp(final WireMockRuntimeInfo wireMockInfo) {
        final HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        final RestClient restClient = RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
        persistenceClient = new PersistenceClient(restClient, wireMockInfo.getHttpBaseUrl());
    }

    @Test
    @DisplayName("Should successfully save transaction to json-server")
    void shouldSaveTransaction() {
        final CardDetails cardDetails = new CardDetails("4532123456783486", "Fonsi Julian", "04/28", "290");
        final Transaction transaction = new Transaction("100", new BigDecimal("340.50"), "T-Shirt", PaymentMethod.DEBIT_CARD, cardDetails);

        stubFor(post(urlEqualTo("/transactions"))
                .willReturn(jsonResponse("""
                        {
                          "id": "100",
                          "value": "340.50",
                          "description": "T-Shirt",
                          "method": "debit_card",
                          "cardNumber": "3486",
                          "cardHolderName": "Fonsi Julian",
                          "cardExpirationDate": "04/28"
                        }
                        """, 201)));

        final Transaction saved = persistenceClient.saveTransaction(transaction);
        assertThat(saved.id()).isEqualTo("100");
    }

    @Test
    @DisplayName("Should successfully save receivable to json-server")
    void shouldSaveReceivable() {
        final Receivable receivable = new Receivable("200", "100", "paid", "2026-07-21", "340.50", "6.81", "333.69");

        stubFor(post(urlEqualTo("/receivables"))
                .willReturn(jsonResponse("""
                        {
                          "id": "200",
                          "transaction_id": "100",
                          "status": "paid",
                          "create_date": "2026-07-21",
                          "subtotal": "340.50",
                          "discount": "6.81",
                          "total": "333.69"
                        }
                        """, 201)));

        final Receivable saved = persistenceClient.saveReceivable(receivable);
        assertThat(saved.id()).isEqualTo("200");
    }

    @Test
    @DisplayName("Should execute compensating DELETE with retries on failure")
    void shouldDeleteTransactionWithRetries() {
        stubFor(delete(urlEqualTo("/transactions/100"))
                .inScenario("Delete Retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("Recovered"));

        stubFor(delete(urlEqualTo("/transactions/100"))
                .inScenario("Delete Retry")
                .whenScenarioStateIs("Recovered")
                .willReturn(aResponse().withStatus(200)));

        assertThatCode(() -> persistenceClient.deleteTransaction("100")).doesNotThrowAnyException();

        verify(2, deleteRequestedFor(urlEqualTo("/transactions/100")));
    }

    @Test
    @DisplayName("Should handle exhausted retries on DELETE and log CRITICAL_AUDIT_ALERT without crashing caller")
    void shouldHandleExhaustedDeleteRetriesGracefully() {
        stubFor(delete(urlEqualTo("/transactions/100"))
                .willReturn(aResponse().withStatus(500)));

        assertThatCode(() -> persistenceClient.deleteTransaction("100")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should fetch transaction by ID or return empty Optional")
    void shouldGetTransactionById() {
        stubFor(get(urlEqualTo("/transactions/100"))
                .willReturn(jsonResponse("""
                        {
                          "id": "100",
                          "value": "340.50",
                          "description": "T-Shirt",
                          "method": "debit_card",
                          "cardNumber": "3486",
                          "cardHolderName": "Fonsi Julian",
                          "cardExpirationDate": "04/28"
                        }
                        """, 200)));

        final Optional<Transaction> found = persistenceClient.getTransactionById("100");
        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo("100");

        stubFor(get(urlEqualTo("/transactions/999"))
                .willReturn(aResponse().withStatus(404)));

        final Optional<Transaction> notFound = persistenceClient.getTransactionById("999");
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("Should fetch all transactions or return empty list on error")
    void shouldGetAllTransactions() {
        stubFor(get(urlEqualTo("/transactions"))
                .willReturn(jsonResponse("""
                        [
                          {
                            "id": "100",
                            "value": "340.50",
                            "description": "T-Shirt",
                            "method": "debit_card",
                            "cardNumber": "3486",
                            "cardHolderName": "Fonsi Julian",
                            "cardExpirationDate": "04/28"
                          }
                        ]
                        """, 200)));

        final List<Transaction> list = persistenceClient.getAllTransactions();
        assertThat(list).hasSize(1);
        assertThat(list.getFirst().id()).isEqualTo("100");

        stubFor(get(urlEqualTo("/transactions"))
                .willReturn(aResponse().withStatus(500)));

        final List<Transaction> emptyList = persistenceClient.getAllTransactions();
        assertThat(emptyList).isEmpty();
    }
}
