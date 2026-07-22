package com.tiendanube.orchestration.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMethodEdgeCasesTest {

    @Test
    @DisplayName("Should handle null total and null date safely in PaymentMethod")
    void shouldHandleNullInputsInPaymentMethod() {
        final PaymentMethod method = PaymentMethod.DEBIT_CARD;

        assertThat(method.calculateFee(null)).isEqualTo(new BigDecimal("0.00"));
        assertThat(method.calculateNetTotal(null)).isEqualTo(new BigDecimal("0.00"));
        assertThat(method.calculatePaymentDate(null)).isAfterOrEqualTo(LocalDate.now());
    }
}
