package com.merchant.orchestration.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter for correlation IDs (X-Request-Id & X-Trace-Id).
 * Features:
 * - 100% Standard Java 21 LTS implementation using SLF4J MDC.
 * - MDC logging context propagation with clean DEBUG-level ingress/egress.
 * - Infrastructure endpoints noise filtering (/actuator, /swagger-ui).
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TracingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(
            @NonNull final HttpServletRequest request,
            @NonNull final HttpServletResponse response,
            @NonNull final FilterChain filterChain
    ) throws ServletException, IOException {

        final long startTime = System.currentTimeMillis();
        final String requestId = extractOrGenerateHeader(request, REQUEST_ID_HEADER);
        final String traceId = extractOrGenerateHeader(request, TRACE_ID_HEADER);

        response.setHeader(REQUEST_ID_HEADER, requestId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        MDC.put("requestId", requestId);
        MDC.put("traceId", traceId);

        final String uri = request.getRequestURI();
        final boolean isInfraEndpoint = isInfrastructureEndpoint(uri);

        if (!isInfraEndpoint) {
            log.debug("HTTP Ingress [{} {}] from IP: {}", request.getMethod(), uri, request.getRemoteAddr());
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            final long duration = System.currentTimeMillis() - startTime;
            if (!isInfraEndpoint) {
                log.debug("HTTP Egress [{} {}] - Status: {} in {} ms",
                        request.getMethod(), uri, response.getStatus(), duration);
            }
            MDC.remove("requestId");
            MDC.remove("traceId");
        }
    }

    private boolean isInfrastructureEndpoint(final String uri) {
        return uri != null && (
                uri.startsWith("/actuator") ||
                uri.startsWith("/swagger-ui") ||
                uri.startsWith("/v3/api-docs") ||
                uri.endsWith(".js") ||
                uri.endsWith(".css") ||
                uri.endsWith(".ico")
        );
    }

    private String extractOrGenerateHeader(final HttpServletRequest request, final String headerName) {
        final String headerValue = request.getHeader(headerName);
        if (StringUtils.isNotBlank(headerValue)) {
            return headerValue.trim();
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
