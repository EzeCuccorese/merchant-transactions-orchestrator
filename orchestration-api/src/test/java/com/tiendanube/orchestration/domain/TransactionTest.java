package com.tiendanube.orchestration.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionTest {

    @Test
    @DisplayName("Should construct Transaction entity preserving masked card number")
    void shouldConstructTransactionWithMaskedCardNumber() {
        final CardDetails cardDetails = new CardDetails("4532123456783486", "Fonsi Julian", "04/28", "290");
        final Transaction transaction = new Transaction(
                "100",
                new BigDecimal("340.50"),
                "T-Shirt Black/M",
                PaymentMethod.DEBIT_CARD,
                cardDetails
        );

        assertThat(transaction.id()).isEqualTo("100");
        assertThat(transaction.value()).isEqualTo("340.50");
        assertThat(transaction.description()).isEqualTo("T-Shirt Black/M");
        assertThat(transaction.method()).isEqualTo(PaymentMethod.DEBIT_CARD);
        assertThat(transaction.cardNumber()).isEqualTo("3486");
        assertThat(transaction.cardHolderName()).isEqualTo("Fonsi Julian");
        assertThat(transaction.cardExpirationDate()).isEqualTo("04/28");
    }

    @Test
    @DisplayName("Should handle null value amount, null method, and null cardDetails gracefully")
    void shouldHandleNullConstructorParameters() {
        final Transaction transaction = new Transaction(
                "101",
                (BigDecimal) null,
                "Test Null",
                (PaymentMethod) null,
                (CardDetails) null
        );

        assertThat(transaction.id()).isEqualTo("101");
        assertThat(transaction.value()).isEqualTo("0.00");
        assertThat(transaction.cardNumber()).isNull();
        assertThat(transaction.cardHolderName()).isNull();
        assertThat(transaction.cardExpirationDate()).isNull();
    }
}
