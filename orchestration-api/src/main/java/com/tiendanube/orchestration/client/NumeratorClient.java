package com.tiendanube.orchestration.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tiendanube.orchestration.domain.exception.NumeratorConflictException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * HTTP Client communicating with external numerator-api to obtain unique sequential IDs
 * using Compare-And-Swap (test-and-set) optimistic locking with exponential backoff and jitter.
 *
 * Features:
 * - Standard 100% Java 21 LTS implementation.
 * - CAS Pair Reservation: generateUniqueIdPair() acquires 2 IDs in a single atomic HTTP call.
 * - Micrometer Metrics: tracks CAS attempts and conflict counters.
 * - Clean DEBUG-level CAS loop logging.
 */
@Slf4j
@Component
public class NumeratorClient {

    private final RestClient restClient;
    private final String numeratorUrl;
    private final int maxAttempts;
    private final int minDelayMs;
    private final int maxDelayMs;
    private final MeterRegistry meterRegistry;

    public NumeratorClient(
            final RestClient restClient,
            @Value("${services.numerator.url:http://localhost:3000}") final String numeratorUrl,
            @Value("${services.numerator.retry.max-attempts:5}") final int maxAttempts,
            @Value("${services.numerator.retry.min-delay-ms:10}") final int minDelayMs,
            @Value("${services.numerator.retry.max-delay-ms:100}") final int maxDelayMs,
            final MeterRegistry meterRegistry
    ) {
        this.restClient = restClient;
        this.numeratorUrl = numeratorUrl;
        this.maxAttempts = maxAttempts;
        this.minDelayMs = minDelayMs;
        this.maxDelayMs = maxDelayMs;
        this.meterRegistry = meterRegistry;
    }

    public record NumeratorResponse(@JsonProperty("numerator") Long numerator, @JsonProperty("currentNumerator") Long currentNumerator, @JsonProperty("error") String error) {}
    public record TestAndSetRequest(@JsonProperty("oldValue") long oldValue, @JsonProperty("newValue") long newValue) {}
    public record IdPair(String firstId, String secondId) {}

    /**
     * Atomically generates a pair of 2 unique sequential string IDs (firstId, secondId)
     * in a single atomic CAS operation (newValue = oldValue + 2), reducing HTTP contention by 50%.
     *
     * @return IdPair containing transactionId (firstId) and receivableId (secondId)
     * @throws NumeratorConflictException if retries are exhausted or service fails
     */
    public IdPair generateUniqueIdPair() {
        final Random random = ThreadLocalRandom.current();
        long currentOldValue = getCurrentNumerator();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            final long candidateValue = currentOldValue + 2;
            log.debug("Attempting numerator CAS pair reservation (attempt {}/{}): oldValue={}, candidateNewValue={}",
                    attempt, maxAttempts, currentOldValue, candidateValue);

            try {
                final NumeratorResponse response = restClient.put()
                        .uri(numeratorUrl + "/numerator/test-and-set")
                        .body(new TestAndSetRequest(currentOldValue, candidateValue))
                        .retrieve()
                        .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                            // Suppress throw on 400 conflict so we can read currentNumerator from response
                        })
                        .body(NumeratorResponse.class);

                if (response != null && response.numerator() != null && response.numerator() > 0) {
                    final long lastAssigned = response.numerator();
                    final String firstId = String.valueOf(lastAssigned - 1);
                    final String secondId = String.valueOf(lastAssigned);

                    log.debug("Acquired CAS ID pair: firstId={}, secondId={} (attempt {})",
                            firstId, secondId, attempt);
                    recordCasAttempts(attempt);
                    return new IdPair(firstId, secondId);
                }

                recordCasConflict();
                if (response != null && response.currentNumerator() != null) {
                    currentOldValue = response.currentNumerator();
                } else {
                    currentOldValue = getCurrentNumerator();
                }
            } catch (Exception e) {
                recordCasConflict();
                log.warn("Error during numerator test-and-set attempt {}/{}: {}", attempt, maxAttempts, e.getMessage());
                currentOldValue = getCurrentNumerator();
            }

            if (attempt < maxAttempts) {
                applyBackoffWithJitter(attempt, random);
            }
        }

        throw new NumeratorConflictException("Failed to acquire unique numerator ID pair after " + maxAttempts + " attempts");
    }

    /**
     * Atomically generates a single unique sequential string ID using test-and-set with retries.
     *
     * @return unique generated string ID
     * @throws NumeratorConflictException if retries are exhausted or service fails
     */
    public String generateUniqueId() {
        final Random random = ThreadLocalRandom.current();
        long currentOldValue = getCurrentNumerator();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            final long candidateValue = currentOldValue + 1;
            log.debug("Attempting numerator test-and-set (attempt {}/{}): oldValue={}, newValue={}",
                    attempt, maxAttempts, currentOldValue, candidateValue);

            try {
                final NumeratorResponse response = restClient.put()
                        .uri(numeratorUrl + "/numerator/test-and-set")
                        .body(new TestAndSetRequest(currentOldValue, candidateValue))
                        .retrieve()
                        .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                            // Suppress throw on 400 conflict so we can read currentNumerator from response
                        })
                        .body(NumeratorResponse.class);

                if (response != null && response.numerator() != null && response.numerator() > 0) {
                    log.debug("Acquired unique numerator ID: {} (attempt {})", response.numerator(), attempt);
                    recordCasAttempts(attempt);
                    return String.valueOf(response.numerator());
                }

                recordCasConflict();
                if (response != null && response.currentNumerator() != null) {
                    currentOldValue = response.currentNumerator();
                } else {
                    currentOldValue = getCurrentNumerator();
                }
            } catch (Exception e) {
                recordCasConflict();
                log.warn("Error during numerator test-and-set attempt {}/{}: {}", attempt, maxAttempts, e.getMessage());
                currentOldValue = getCurrentNumerator();
            }

            if (attempt < maxAttempts) {
                applyBackoffWithJitter(attempt, random);
            }
        }

        throw new NumeratorConflictException("Failed to acquire unique numerator ID after " + maxAttempts + " attempts");
    }

    private long getCurrentNumerator() {
        try {
            final NumeratorResponse response = restClient.get()
                    .uri(numeratorUrl + "/numerator")
                    .retrieve()
                    .body(NumeratorResponse.class);
            if (response != null && response.numerator() != null) {
                return response.numerator();
            }
        } catch (Exception e) {
            log.error("Failed to fetch current numerator value from {}", numeratorUrl, e);
        }
        return 0;
    }

    private void applyBackoffWithJitter(final int attempt, final Random random) {
        final int expDelay = Math.min(maxDelayMs, minDelayMs * (1 << (attempt - 1)));
        final int jitter = random.nextInt(Math.max(1, expDelay / 2));
        final int sleepTime = Math.min(maxDelayMs, expDelay + jitter);
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void recordCasAttempts(final int attempts) {
        if (meterRegistry != null) {
            DistributionSummary.builder("numerator.cas.attempts")
                    .description("Distribution of CAS attempts taken to acquire unique IDs")
                    .register(meterRegistry)
                    .record(attempts);
        }
    }

    private void recordCasConflict() {
        if (meterRegistry != null) {
            Counter.builder("numerator.cas.conflicts.total")
                    .description("Total number of CAS 400 conflicts encountered")
                    .register(meterRegistry)
                    .increment();
        }
    }
}
