package com.merchant.orchestration.config;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.web.client.RestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@WireMockTest
class RestClientConfigTest {

    @Test
    @DisplayName("Should create RestClient bean and execute request with correlation tracing interceptor headers")
    void shouldExecuteInterceptorWithMdcTracingHeaders(final WireMockRuntimeInfo wireMockInfo) {
        final RestClientConfig config = new RestClientConfig();
        final RestClient restClient = config.restClient(5000, 5000);

        stubFor(get(urlEqualTo("/test-interceptor"))
                .withHeader("X-Request-Id", equalTo("req-test-123"))
                .withHeader("X-Trace-Id", equalTo("trace-test-456"))
                .willReturn(aResponse().withStatus(200)));

        MDC.put("requestId", "req-test-123");
        MDC.put("traceId", "trace-test-456");
        try {
            restClient.get()
                    .uri(wireMockInfo.getHttpBaseUrl() + "/test-interceptor")
                    .retrieve()
                    .toBodilessEntity();
        } finally {
            MDC.clear();
        }

        verify(getRequestedFor(urlEqualTo("/test-interceptor"))
                .withHeader("X-Request-Id", equalTo("req-test-123"))
                .withHeader("X-Trace-Id", equalTo("trace-test-456")));
    }
}
