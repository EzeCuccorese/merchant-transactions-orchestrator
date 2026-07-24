package com.merchant.orchestration.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessExceptionTest {

    @Test
    @DisplayName("Should preserve cause and error codes in BusinessException subclasses")
    void shouldPreserveExceptionFields() {
        final Throwable cause = new IllegalArgumentException("Root cause");
        final NumeratorConflictException ex1 = new NumeratorConflictException("Conflict", cause);

        assertThat(ex1.getErrorCode()).isEqualTo("NUMERATOR_CONFLICT");
        assertThat(ex1.getCause()).isEqualTo(cause);

        final InvalidPaymentMethodException ex2 = new InvalidPaymentMethodException("unknown");
        assertThat(ex2.getErrorCode()).isEqualTo("INVALID_PAYMENT_METHOD");
    }
}
