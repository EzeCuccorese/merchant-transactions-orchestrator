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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "springdoc.swagger-ui.enabled=false"
})
@AutoConfigureTestRestTemplate
class OrchestrationIntegrationTest {

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
    @DisplayName("Integration Test: Debit Card Transaction End-to-End Happy Path (2% fee, D+0 date, paid status)")
    void shouldOrchestrateDebitCardTransactionHappyPath() {
        // 1. Numerator mocks for single-step CAS Pair Reservation (oldValue: 99, newValue: 101 => firstId: 100, secondId: 101)
        stubFor(get(urlEqualTo("/numerator"))
                .willReturn(jsonResponse("{\"numerator\": 99}", 200)));

        stubFor(put(urlEqualTo("/numerator/test-and-set"))
                .withRequestBody(equalToJson("{\"oldValue\": 99, \"newValue\": 101}"))
                .willReturn(jsonResponse("{\"numerator\": 101}", 200)));

        // 2. Persistence mocks for POST /transactions and POST /receivables
        stubFor(post(urlEqualTo("/transactions"))
                .withRequestBody(matchingJsonPath("$.cardNumber", equalTo("3486")))
                .willReturn(jsonResponse("""
                        {
                          "id": "100",
                          "value": "340.50",
                          "description": "T-Shirt Black/M",
                          "method": "debit_card",
                          "cardNumber": "3486",
                          "cardHolderName": "Fonsi Julian",
                          "cardExpirationDate": "04/28"
                        }
                        """, 201)));

        stubFor(post(urlEqualTo("/receivables"))
                .withRequestBody(matchingJsonPath("$.transaction_id", equalTo("100")))
                .withRequestBody(matchingJsonPath("$.status", equalTo("paid")))
                .willReturn(jsonResponse("""
                        {
                          "id": "101",
                          "transaction_id": "100",
                          "status": "paid",
                          "create_date": "%s",
                          "subtotal": "340.50",
                          "discount": "6.81",
                          "total": "333.69"
                        }
                        """.formatted(LocalDate.now()), 201)));

        final String requestJson = """
                {
                  "value": "340.50",
                  "description": "T-Shirt Black/M",
                  "method": "debit_card",
                  "cardNumber": "4532123456783486",
                  "cardHolderName": "Fonsi Julian",
                  "cardExpirationDate": "04/28",
                  "cardCvv": "290"
                }
                """;

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

        final ResponseEntity<String> response = restTemplate.postForEntity("/transactions", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        final String responseBody = response.getBody();
        assertThat(responseBody).isNotNull();

        // Strict JSON Path Contract Verification
        assertThat(JsonPath.<String>read(responseBody, "$.transaction.id")).isEqualTo("100");
        assertThat(JsonPath.<String>read(responseBody, "$.transaction.value")).isEqualTo("340.50");
        assertThat(JsonPath.<String>read(responseBody, "$.transaction.method")).isEqualTo("debit_card");
        assertThat(JsonPath.<String>read(responseBody, "$.transaction.cardNumber")).isEqualTo("3486");

        assertThat(JsonPath.<String>read(responseBody, "$.receivable.id")).isEqualTo("101");
        assertThat(JsonPath.<String>read(responseBody, "$.receivable.transaction_id")).isEqualTo("100");
        assertThat(JsonPath.<String>read(responseBody, "$.receivable.status")).isEqualTo("paid");
        assertThat(JsonPath.<String>read(responseBody, "$.receivable.subtotal")).isEqualTo("340.50");
        assertThat(JsonPath.<String>read(responseBody, "$.receivable.discount")).isEqualTo("6.81");
        assertThat(JsonPath.<String>read(responseBody, "$.receivable.total")).isEqualTo("333.69");
        assertThat(JsonPath.<String>read(responseBody, "$.receivable.create_date")).isEqualTo(LocalDate.now().toString());
    }

    @Test
    @DisplayName("Integration Test: Credit Card Transaction End-to-End Happy Path (4% fee, D+30 date, waiting_funds status)")
    void shouldOrchestrateCreditCardTransactionHappyPath() {
        stubFor(get(urlEqualTo("/numerator"))
                .willReturn(jsonResponse("{\"numerator\": 299}", 200)));

        stubFor(put(urlEqualTo("/numerator/test-and-set"))
                .withRequestBody(equalToJson("{\"oldValue\": 299, \"newValue\": 301}"))
                .willReturn(jsonResponse("{\"numerator\": 301}", 200)));

        final LocalDate expectedPaymentDate = LocalDate.now().plusDays(30);

        stubFor(post(urlEqualTo("/transactions"))
                .withRequestBody(matchingJsonPath("$.cardNumber", equalTo("7890")))
                .willReturn(jsonResponse("""
                        {
                          "id": "300",
                          "value": "250.00",
                          "description": "Jeans Slim Fit",
                          "method": "credit_card",
                          "cardNumber": "7890",
                          "cardHolderName": "Simplenube Store",
                          "cardExpirationDate": "11/29"
                        }
                        """, 201)));

        stubFor(post(urlEqualTo("/receivables"))
                .withRequestBody(matchingJsonPath("$.transaction_id", equalTo("300")))
                .withRequestBody(matchingJsonPath("$.status", equalTo("waiting_funds")))
                .willReturn(jsonResponse("""
                        {
                          "id": "301",
                          "transaction_id": "300",
                          "status": "waiting_funds",
                          "create_date": "%s",
                          "subtotal": "250.00",
                          "discount": "10.00",
                          "total": "240.00"
                        }
                        """.formatted(expectedPaymentDate), 201)));

        final String requestJson = """
                {
                  "value": "250.00",
                  "description": "Jeans Slim Fit",
                  "method": "credit_card",
                  "cardNumber": "5412751234567890",
                  "cardHolderName": "Simplenube Store",
                  "cardExpirationDate": "11/29",
                  "cardCvv": "888"
                }
                """;

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

        final ResponseEntity<String> response = restTemplate.postForEntity("/transactions", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        final String responseBody = response.getBody();
        assertThat(responseBody).isNotNull();

        assertThat(JsonPath.<String>read(responseBody, "$.transaction.id")).isEqualTo("300");
        assertThat(JsonPath.<String>read(responseBody, "$.receivable.id")).isEqualTo("301");
        assertThat(JsonPath.<String>read(responseBody, "$.receivable.status")).isEqualTo("waiting_funds");
        assertThat(JsonPath.<String>read(responseBody, "$.receivable.discount")).isEqualTo("10.00");
        assertThat(JsonPath.<String>read(responseBody, "$.receivable.total")).isEqualTo("240.00");
        assertThat(JsonPath.<String>read(responseBody, "$.receivable.create_date")).isEqualTo(expectedPaymentDate.toString());
    }

    @Test
    @DisplayName("Integration Test: Query Transaction by ID Happy Path")
    void shouldGetTransactionByIdHappyPath() {
        stubFor(get(urlEqualTo("/transactions/100"))
                .willReturn(jsonResponse("""
                        {
                          "id": "100",
                          "value": "340.50",
                          "description": "T-Shirt Black/M",
                          "method": "debit_card",
                          "cardNumber": "3486",
                          "cardHolderName": "Fonsi Julian",
                          "cardExpirationDate": "04/28"
                        }
                        """, 200)));

        final ResponseEntity<String> response = restTemplate.getForEntity("/transactions/100", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        final String responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(JsonPath.<String>read(responseBody, "$.id")).isEqualTo("100");
        assertThat(JsonPath.<String>read(responseBody, "$.cardNumber")).isEqualTo("3486");
    }

    @Test
    @DisplayName("Integration Test: Query All Transactions Happy Path")
    void shouldGetAllTransactionsHappyPath() {
        stubFor(get(urlEqualTo("/transactions"))
                .willReturn(jsonResponse("""
                        [
                          {
                            "id": "100",
                            "value": "340.50",
                            "description": "T-Shirt Black/M",
                            "method": "debit_card",
                            "cardNumber": "3486",
                            "cardHolderName": "Fonsi Julian",
                            "cardExpirationDate": "04/28"
                          }
                        ]
                        """, 200)));

        final ResponseEntity<String> response = restTemplate.getForEntity("/transactions", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        final String responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(JsonPath.<Integer>read(responseBody, "$.length()")).isEqualTo(1);
        assertThat(JsonPath.<String>read(responseBody, "$[0].id")).isEqualTo("100");
    }
}
