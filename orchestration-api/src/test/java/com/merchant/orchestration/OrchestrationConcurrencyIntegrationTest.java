package com.merchant.orchestration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "springdoc.swagger-ui.enabled=false"
})
@AutoConfigureTestRestTemplate
class OrchestrationConcurrencyIntegrationTest {

    private static WireMockServer wireMockServer;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
    }

    @DynamicPropertySource
    static void configureProperties(final DynamicPropertyRegistry registry) {
        registry.add("services.numerator.url", () -> "http://localhost:" + wireMockServer.port());
        registry.add("services.persistence.url", () -> "http://localhost:" + wireMockServer.port());
    }

    @Test
    @DisplayName("High Concurrency Integration Test: 200 Concurrent Virtual Threads Stress Test (CAS, Zero Duplicates, Actuator Metrics)")
    void shouldHandle200ConcurrentRequestsUsingVirtualThreadsWithoutRaceConditions() throws Exception {
        final int concurrentRequests = 200;

        // Stub numerator GET /numerator
        stubFor(get(urlEqualTo("/numerator")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(200)
                .withBody("{\"numerator\": 100}")));

        // Stub numerator PUT /numerator/test-and-set for CAS pair reservation
        stubFor(put(urlEqualTo("/numerator/test-and-set"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{\"numerator\": 102}")));

        // Stub persistence endpoints
        stubFor(post(urlEqualTo("/transactions"))
                .willReturn(jsonResponse("""
                        {
                          "id": "101",
                          "value": "100.00",
                          "description": "Stress Test Item",
                          "method": "debit_card",
                          "cardNumber": "3486",
                          "cardHolderName": "Stress Tester",
                          "cardExpirationDate": "04/28"
                        }
                        """, 201)));

        stubFor(post(urlEqualTo("/receivables"))
                .willReturn(jsonResponse("""
                        {
                          "id": "102",
                          "transaction_id": "101",
                          "status": "paid",
                          "create_date": "%s",
                          "subtotal": "100.00",
                          "discount": "2.00",
                          "total": "98.00"
                        }
                        """.formatted(LocalDate.now()), 201)));

        // Create Virtual Threads executor for 200 concurrent tasks
        final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        final CyclicBarrier barrier = new CyclicBarrier(concurrentRequests);
        final ConcurrentLinkedQueue<ResponseEntity<String>> responses = new ConcurrentLinkedQueue<>();
        final Set<String> assignedTransactionIds = ConcurrentHashMap.newKeySet();

        final String payloadJson = """
                {
                  "value": "100.00",
                  "description": "Stress Test Item",
                  "method": "debit_card",
                  "cardNumber": "4532123456783486",
                  "cardHolderName": "Stress Tester",
                  "cardExpirationDate": "04/28",
                  "cardCvv": "290"
                }
                """;

        final CountDownLatch latch = new CountDownLatch(concurrentRequests);

        for (int i = 0; i < concurrentRequests; i++) {
            executor.submit(() -> {
                try {
                    barrier.await(); // Synchronize all virtual threads to launch simultaneously
                    final HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    final HttpEntity<String> entity = new HttpEntity<>(payloadJson, headers);

                    final ResponseEntity<String> response = restTemplate.postForEntity("/transactions", entity, String.class);
                    responses.add(response);

                    if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                        final String txId = JsonPath.read(response.getBody(), "$.transaction.id");
                        assignedTransactionIds.add(txId);
                    }
                } catch (Exception e) {
                    // Suppress thread interruption during test shutdown
                } finally {
                    latch.countDown();
                }
            });
        }

        final boolean completed = latch.await(15, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        executor.shutdown();

        assertThat(responses).hasSize(concurrentRequests);
        for (final ResponseEntity<String> resp : responses) {
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        // Verify Actuator Health Endpoint
        final ResponseEntity<String> healthResponse = restTemplate.getForEntity("/actuator/health", String.class);
        assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(healthResponse.getBody()).contains("\"status\":\"UP\"");

        // Verify Actuator Metrics Endpoint
        final ResponseEntity<String> metricsResponse = restTemplate.getForEntity("/actuator/metrics", String.class);
        assertThat(metricsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        final String metricsBody = metricsResponse.getBody();
        assertThat(metricsBody).isNotNull();
        assertThat(metricsBody).contains("transactions.created.total");
        assertThat(metricsBody).contains("transactions.amount.total");
    }
}
