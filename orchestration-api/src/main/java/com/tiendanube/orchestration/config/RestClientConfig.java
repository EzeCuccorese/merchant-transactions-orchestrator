package com.tiendanube.orchestration.config;

import com.tiendanube.orchestration.filter.TracingFilter;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Spring Configuration providing a cross-cutting RestClient bean with Java 21+ JdkClientHttpRequestFactory
 * forced to HTTP 1.1 for maximum compatibility with json-server & WireMock,
 * timeouts, default headers, and correlation header tracing interceptor.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient(
            final RestClient.Builder builder,
            @Value("${http.client.connect-timeout-ms:5000}") final int connectTimeoutMs,
            @Value("${http.client.read-timeout-ms:5000}") final int readTimeoutMs
    ) {
        final HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();

        final JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        final ClientHttpRequestInterceptor tracingInterceptor = (request, body, execution) -> {
            final String requestId = MDC.get("requestId");
            final String traceId = MDC.get("traceId");

            if (requestId != null) {
                request.getHeaders().add(TracingFilter.REQUEST_ID_HEADER, requestId);
            }
            if (traceId != null) {
                request.getHeaders().add(TracingFilter.TRACE_ID_HEADER, traceId);
            }

            return execution.execute(request, body);
        };

        return builder
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .requestInterceptor(tracingInterceptor)
                .build();
    }
}
