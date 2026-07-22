package com.tiendanube.orchestration.client;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.tiendanube.orchestration.domain.exception.NumeratorConflictException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WireMockTest
class NumeratorClientTest {

    private NumeratorClient numeratorClient;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp(final WireMockRuntimeInfo wireMockInfo) {
        final HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        final RestClient restClient = RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
        meterRegistry = new SimpleMeterRegistry();
        numeratorClient = new NumeratorClient(
                restClient,
                wireMockInfo.getHttpBaseUrl(),
                3,
                5,
                20,
                meterRegistry
        );
    }

    @Test
    @DisplayName("Should test inner records NumeratorResponse, TestAndSetRequest, and IdPair")
    void shouldTestInnerRecords() {
        final NumeratorClient.NumeratorResponse resp1 = new NumeratorClient.NumeratorResponse(10L, 5L, "err");
        final NumeratorClient.NumeratorResponse resp2 = new NumeratorClient.NumeratorResponse(10L, 5L, "err");
        assertThat(resp1.numerator()).isEqualTo(10L);
        assertThat(resp1.currentNumerator()).isEqualTo(5L);
        assertThat(resp1.error()).isEqualTo("err");
        assertThat(resp1).isEqualTo(resp2);
        assertThat(resp1.hashCode()).isEqualTo(resp2.hashCode());
        assertThat(resp1.toString()).contains("10");

        final NumeratorClient.TestAndSetRequest req1 = new NumeratorClient.TestAndSetRequest(10L, 11L);
        final NumeratorClient.TestAndSetRequest req2 = new NumeratorClient.TestAndSetRequest(10L, 11L);
        assertThat(req1.oldValue()).isEqualTo(10L);
        assertThat(req1.newValue()).isEqualTo(11L);
        assertThat(req1).isEqualTo(req2);
        assertThat(req1.hashCode()).isEqualTo(req2.hashCode());
        assertThat(req1.toString()).contains("10");

        final NumeratorClient.IdPair pair1 = new NumeratorClient.IdPair("10", "11");
        final NumeratorClient.IdPair pair2 = new NumeratorClient.IdPair("10", "11");
        assertThat(pair1.firstId()).isEqualTo("10");
        assertThat(pair1.secondId()).isEqualTo("11");
        assertThat(pair1).isEqualTo(pair2);
        assertThat(pair1.hashCode()).isEqualTo(pair2.hashCode());
        assertThat(pair1.toString()).contains("10");
    }

    @Test
    @DisplayName("Should successfully acquire unique ID pair in a single atomic CAS call")
    void shouldAcquireUniqueIdPairInSingleCasCall() {
        stubFor(get(urlEqualTo("/numerator"))
                .willReturn(jsonResponse("{\"numerator\": 99}", 200)));

        stubFor(put(urlEqualTo("/numerator/test-and-set"))
                .withRequestBody(equalToJson("{\"oldValue\": 99, \"newValue\": 101}"))
                .willReturn(jsonResponse("{\"numerator\": 101}", 200)));

        final NumeratorClient.IdPair pair = numeratorClient.generateUniqueIdPair();
        assertThat(pair.firstId()).isEqualTo("100");
        assertThat(pair.secondId()).isEqualTo("101");
    }

    @Test
    @DisplayName("Should retry generateUniqueIdPair on CAS conflict and succeed on second attempt")
    void shouldRetryPairGenerationOnConflictAndSucceed() {
        stubFor(get(urlEqualTo("/numerator"))
                .willReturn(jsonResponse("{\"numerator\": 99}", 200)));

        stubFor(put(urlEqualTo("/numerator/test-and-set"))
                .withRequestBody(equalToJson("{\"oldValue\": 99, \"newValue\": 101}"))
                .willReturn(jsonResponse("{\"error\": \"Conflict\", \"currentNumerator\": 101}", 400)));

        stubFor(put(urlEqualTo("/numerator/test-and-set"))
                .withRequestBody(equalToJson("{\"oldValue\": 101, \"newValue\": 103}"))
                .willReturn(jsonResponse("{\"numerator\": 103}", 200)));

        final NumeratorClient.IdPair pair = numeratorClient.generateUniqueIdPair();
        assertThat(pair.firstId()).isEqualTo("102");
        assertThat(pair.secondId()).isEqualTo("103");
    }

    @Test
    @DisplayName("Should handle 500 server error on generateUniqueIdPair and retry successfully")
    void shouldHandleServerErrorOnPairGenerationAndRetry() {
        stubFor(get(urlEqualTo("/numerator"))
                .willReturn(jsonResponse("{\"numerator\": 99}", 200)));

        stubFor(put(urlEqualTo("/numerator/test-and-set"))
                .inScenario("Pair Server Error Retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("Recovered"));

        stubFor(put(urlEqualTo("/numerator/test-and-set"))
                .inScenario("Pair Server Error Retry")
                .whenScenarioStateIs("Recovered")
                .willReturn(jsonResponse("{\"numerator\": 101}", 200)));

        final NumeratorClient.IdPair pair = numeratorClient.generateUniqueIdPair();
        assertThat(pair.firstId()).isEqualTo("100");
        assertThat(pair.secondId()).isEqualTo("101");
    }

    @Test
    @DisplayName("Should throw NumeratorConflictException when generateUniqueIdPair retries are exhausted")
    void shouldThrowExceptionWhenPairRetriesExhausted() {
        stubFor(get(urlEqualTo("/numerator"))
                .willReturn(jsonResponse("{\"numerator\": 99}", 200)));

        stubFor(put(urlEqualTo("/numerator/test-and-set"))
                .willReturn(jsonResponse("{\"error\": \"Conflict\", \"currentNumerator\": 99}", 400)));

        assertThatThrownBy(() -> numeratorClient.generateUniqueIdPair())
                .isInstanceOf(NumeratorConflictException.class)
                .hasMessageContaining("Failed to acquire unique numerator ID pair after 3 attempts");
    }

    @Test
    @DisplayName("Should successfully acquire single unique ID when test-and-set succeeds on first attempt")
    void shouldAcquireUniqueIdOnFirstAttempt() {
        stubFor(get(urlEqualTo("/numerator"))
                .willReturn(jsonResponse("{\"numerator\": 42}", 200)));

        stubFor(put(urlEqualTo("/numerator/test-and-set"))
                .withRequestBody(equalToJson("{\"oldValue\": 42, \"newValue\": 43}"))
                .willReturn(jsonResponse("{\"numerator\": 43}", 200)));

        final String generatedId = numeratorClient.generateUniqueId();
        assertThat(generatedId).isEqualTo("43");
    }

    @Test
    @DisplayName("Should retry with exponential backoff on CAS conflict and succeed on second attempt")
    void shouldRetryAndSucceedOnConflict() {
        stubFor(get(urlEqualTo("/numerator"))
                .willReturn(jsonResponse("{\"numerator\": 42}", 200)));

        // First attempt fails with 400 conflict
        stubFor(put(urlEqualTo("/numerator/test-and-set"))
                .withRequestBody(equalToJson("{\"oldValue\": 42, \"newValue\": 43}"))
                .willReturn(jsonResponse("{\"error\": \"Conflict\", \"currentNumerator\": 43}", 400)));

        // Second attempt succeeds with oldValue=43
        stubFor(put(urlEqualTo("/numerator/test-and-set"))
                .withRequestBody(equalToJson("{\"oldValue\": 43, \"newValue\": 44}"))
                .willReturn(jsonResponse("{\"numerator\": 44}", 200)));

        final String generatedId = numeratorClient.generateUniqueId();
        assertThat(generatedId).isEqualTo("44");
        assertThat(meterRegistry.find("numerator.cas.conflicts.total").counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should handle 500 exception on PUT /numerator/test-and-set and retry successfully")
    void shouldHandleServerErrorOnTestAndSetAndRetry() {
        stubFor(get(urlEqualTo("/numerator"))
                .willReturn(jsonResponse("{\"numerator\": 10}", 200)));

        stubFor(put(urlEqualTo("/numerator/test-and-set"))
                .inScenario("Server Error Retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("Recovered"));

        stubFor(put(urlEqualTo("/numerator/test-and-set"))
                .inScenario("Server Error Retry")
                .whenScenarioStateIs("Recovered")
                .willReturn(jsonResponse("{\"numerator\": 11}", 200)));

        final String generatedId = numeratorClient.generateUniqueId();
        assertThat(generatedId).isEqualTo("11");
    }

    @Test
    @DisplayName("Should handle empty JSON response on test-and-set by re-fetching GET /numerator")
    void shouldHandleEmptyJsonResponseOnTestAndSet() {
        stubFor(get(urlEqualTo("/numerator"))
                .willReturn(jsonResponse("{\"numerator\": 10}", 200)));

        stubFor(put(urlEqualTo("/numerator/test-and-set"))
                .inScenario("Empty JSON Retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(jsonResponse("{}", 200))
                .willSetStateTo("Recovered"));

        stubFor(put(urlEqualTo("/numerator/test-and-set"))
                .inScenario("Empty JSON Retry")
                .whenScenarioStateIs("Recovered")
                .willReturn(jsonResponse("{\"numerator\": 11}", 200)));

        final String generatedId = numeratorClient.generateUniqueId();
        assertThat(generatedId).isEqualTo("11");
    }

    @Test
    @DisplayName("Should handle 500 error on GET /numerator gracefully")
    void shouldHandleGetNumeratorFailureGracefully() {
        stubFor(get(urlEqualTo("/numerator"))
                .willReturn(aResponse().withStatus(500)));

        stubFor(put(urlEqualTo("/numerator/test-and-set"))
                .withRequestBody(equalToJson("{\"oldValue\": 0, \"newValue\": 1}"))
                .willReturn(jsonResponse("{\"numerator\": 1}", 200)));

        final String generatedId = numeratorClient.generateUniqueId();
        assertThat(generatedId).isEqualTo("1");
    }

    @Test
    @DisplayName("Should throw NumeratorConflictException when retries are exhausted")
    void shouldThrowExceptionWhenMaxAttemptsExhausted() {
        stubFor(get(urlEqualTo("/numerator"))
                .willReturn(jsonResponse("{\"numerator\": 42}", 200)));

        stubFor(put(urlEqualTo("/numerator/test-and-set"))
                .willReturn(jsonResponse("{\"error\": \"Conflict\", \"currentNumerator\": 42}", 400)));

        assertThatThrownBy(() -> numeratorClient.generateUniqueId())
                .isInstanceOf(NumeratorConflictException.class)
                .hasMessageContaining("Failed to acquire unique numerator ID after 3 attempts");
    }
}
