package com.merchant.orchestration.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ReceivableTest {

    @Test
    @DisplayName("Should create Receivable for credit_card transaction with 4% fee and D+30 date")
    void shouldCreateCreditCardReceivableCorrectly() {
        final LocalDate creationDate = LocalDate.of(2026, 7, 21);
        final Receivable receivable = Receivable.fromTransaction(
                "200",
                "100",
                new BigDecimal("250.00"),
                PaymentMethod.CREDIT_CARD,
                creationDate
        );

        assertThat(receivable.id()).isEqualTo("200");
        assertThat(receivable.transactionId()).isEqualTo("100");
        assertThat(receivable.status()).isEqualTo("waiting_funds");
        assertThat(receivable.createDate()).isEqualTo("2026-08-20");
        assertThat(receivable.subtotal()).isEqualTo("250.00");
        assertThat(receivable.discount()).isEqualTo("10.00");
        assertThat(receivable.total()).isEqualTo("240.00");
    }

    @Test
    @DisplayName("Should create Receivable for debit_card transaction with 2% fee and D+0 date")
    void shouldCreateDebitCardReceivableCorrectly() {
        final LocalDate creationDate = LocalDate.of(2026, 7, 21);
        final Receivable receivable = Receivable.fromTransaction(
                "201",
                "101",
                new BigDecimal("340.50"),
                PaymentMethod.DEBIT_CARD,
                creationDate
        );

        assertThat(receivable.id()).isEqualTo("201");
        assertThat(receivable.transactionId()).isEqualTo("101");
        assertThat(receivable.status()).isEqualTo("paid");
        assertThat(receivable.createDate()).isEqualTo("2026-07-21");
        assertThat(receivable.subtotal()).isEqualTo("340.50");
        assertThat(receivable.discount()).isEqualTo("6.81");
        assertThat(receivable.total()).isEqualTo("333.69");
    }
}
