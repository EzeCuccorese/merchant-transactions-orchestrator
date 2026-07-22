package com.tiendanube.orchestration.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

class TracingFilterTest {

    @Test
    @DisplayName("Should preserve existing X-Request-Id header and set in response and MDC")
    void shouldPreserveExistingTracingHeaders() throws ServletException, IOException {
        final TracingFilter filter = new TracingFilter();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "req-12345");
        request.addHeader("X-Trace-Id", "trace-67890");

        final MockHttpServletResponse response = new MockHttpServletResponse();
        final FilterChain filterChain = Mockito.mock(FilterChain.class);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getHeader("X-Request-Id")).isEqualTo("req-12345");
        assertThat(response.getHeader("X-Trace-Id")).isEqualTo("trace-67890");
        assertThat(MDC.get("requestId")).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should generate X-Request-Id header when not present in incoming request")
    void shouldGenerateTracingHeadersWhenMissing() throws ServletException, IOException {
        final TracingFilter filter = new TracingFilter();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final FilterChain filterChain = Mockito.mock(FilterChain.class);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getHeader("X-Request-Id")).isNotBlank();
        assertThat(response.getHeader("X-Trace-Id")).isNotBlank();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should unwrap and rethrow ServletException thrown in chain")
    void shouldUnwrapServletException() throws ServletException, IOException {
        final TracingFilter filter = new TracingFilter();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final FilterChain filterChain = Mockito.mock(FilterChain.class);

        doThrow(new ServletException("Chain error")).when(filterChain).doFilter(any(), any());

        assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChain))
                .isInstanceOf(ServletException.class)
                .hasMessage("Chain error");
    }

    @Test
    @DisplayName("Should unwrap and rethrow IOException thrown in chain")
    void shouldUnwrapIOException() throws ServletException, IOException {
        final TracingFilter filter = new TracingFilter();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final FilterChain filterChain = Mockito.mock(FilterChain.class);

        doThrow(new IOException("IO error")).when(filterChain).doFilter(any(), any());

        assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChain))
                .isInstanceOf(IOException.class)
                .hasMessage("IO error");
    }

    @Test
    @DisplayName("Should rethrow generic RuntimeException thrown in chain")
    void shouldRethrowRuntimeException() throws ServletException, IOException {
        final TracingFilter filter = new TracingFilter();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final FilterChain filterChain = Mockito.mock(FilterChain.class);

        doThrow(new IllegalStateException("State error")).when(filterChain).doFilter(any(), any());

        assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChain))
                .isInstanceOf(RuntimeException.class);
    }
}
