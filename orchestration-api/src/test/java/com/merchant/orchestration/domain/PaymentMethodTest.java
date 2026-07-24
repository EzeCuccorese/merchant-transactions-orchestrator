package com.merchant.orchestration.domain;

import com.merchant.orchestration.domain.exception.InvalidPaymentMethodException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentMethodTest {

    @Test
    @DisplayName("Should calculate correct fee and status for debit_card (2% fee, paid status, D+0)")
    void shouldCalculateDebitCardRulesCorrectly() {
        final PaymentMethod method = PaymentMethod.fromCode("debit_card");
        final BigDecimal transactionValue = new BigDecimal("340.50");
        final LocalDate creationDate = LocalDate.of(2026, 7, 21);

        final BigDecimal fee = method.calculateFee(transactionValue);
        final BigDecimal netTotal = method.calculateNetTotal(transactionValue);
        final LocalDate paymentDate = method.calculatePaymentDate(creationDate);

        assertThat(method.getStatus()).isEqualTo("paid");
        assertThat(fee).isEqualTo(new BigDecimal("6.81"));
        assertThat(netTotal).isEqualTo(new BigDecimal("333.69"));
        assertThat(paymentDate).isEqualTo(LocalDate.of(2026, 7, 21));
    }

    @Test
    @DisplayName("Should calculate correct fee and status for credit_card (4% fee, waiting_funds status, D+30)")
    void shouldCalculateCreditCardRulesCorrectly() {
        final PaymentMethod method = PaymentMethod.fromCode("credit_card");
        final BigDecimal transactionValue = new BigDecimal("100.00");
        final LocalDate creationDate = LocalDate.of(2026, 7, 21);

        final BigDecimal fee = method.calculateFee(transactionValue);
        final BigDecimal netTotal = method.calculateNetTotal(transactionValue);
        final LocalDate paymentDate = method.calculatePaymentDate(creationDate);

        assertThat(method.getStatus()).isEqualTo("waiting_funds");
        assertThat(fee).isEqualTo(new BigDecimal("4.00"));
        assertThat(netTotal).isEqualTo(new BigDecimal("96.00"));
        assertThat(paymentDate).isEqualTo(LocalDate.of(2026, 8, 20));
    }

    @Test
    @DisplayName("Should test all PaymentMethod getters")
    void shouldReturnCorrectGetterValues() {
        final PaymentMethod debit = PaymentMethod.DEBIT_CARD;
        assertThat(debit.getCode()).isEqualTo("debit_card");
        assertThat(debit.getStatus()).isEqualTo("paid");
        assertThat(debit.getFeePercentage()).isEqualTo(new BigDecimal("0.02"));
        assertThat(debit.getSettlementDays()).isEqualTo(0);
    }

    @Test
    @DisplayName("Edge Case: null values handling in fee, net total, and payment date calculations")
    void shouldHandleNullValuesInDomainCalculationsGracefully() {
        final PaymentMethod method = PaymentMethod.DEBIT_CARD;

        assertThat(method.calculateFee(null)).isEqualTo(new BigDecimal("0.00"));
        assertThat(method.calculateNetTotal(null)).isEqualTo(new BigDecimal("0.00"));
        assertThat(method.calculatePaymentDate(null)).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("Edge Case: case insensitivity and whitespace trimming when fetching payment method by code")
    void shouldParsePaymentMethodCaseInsensitivelyWithWhitespace() {
        final PaymentMethod debit = PaymentMethod.fromCode("   DEBIT_CARD  ");
        final PaymentMethod credit = PaymentMethod.fromCode("CrEdIt_CaRd");

        assertThat(debit).isEqualTo(PaymentMethod.DEBIT_CARD);
        assertThat(credit).isEqualTo(PaymentMethod.CREDIT_CARD);
    }

    @Test
    @DisplayName("Edge Case: RoundingMode.HALF_UP rounding precision for fractional cent amounts")
    void shouldRoundHalfUpOnFractionalCents() {
        final PaymentMethod method = PaymentMethod.DEBIT_CARD; // 2% fee
        final BigDecimal value = new BigDecimal("100.125"); // 2% of 100.125 = 2.0025 => 2.00

        final BigDecimal fee = method.calculateFee(value);
        assertThat(fee).isEqualTo(new BigDecimal("2.00"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid_card", "bitcoin", "", " "})
    @DisplayName("Should throw InvalidPaymentMethodException for unknown or invalid payment method codes")
    void shouldThrowExceptionForInvalidPaymentMethod(final String invalidCode) {
        assertThatThrownBy(() -> PaymentMethod.fromCode(invalidCode))
                .isInstanceOf(InvalidPaymentMethodException.class)
                .hasMessageContaining("Unsupported payment method");
    }

    @Test
    @DisplayName("Edge Case: null payment method code should throw InvalidPaymentMethodException")
    void shouldThrowExceptionForNullPaymentMethodCode() {
        assertThatThrownBy(() -> PaymentMethod.fromCode(null))
                .isInstanceOf(InvalidPaymentMethodException.class);
    }
}
