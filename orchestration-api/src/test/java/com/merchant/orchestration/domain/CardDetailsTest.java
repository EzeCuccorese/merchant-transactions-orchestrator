package com.merchant.orchestration.domain;

import com.merchant.orchestration.domain.exception.CardValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CardDetailsTest {

    @Test
    @DisplayName("Should extract last 4 digits of card number correctly and mask first 12 digits")
    void shouldMaskCardNumberToLastFourDigits() {
        final CardDetails card = new CardDetails("4532 1234 5678 3486", "Fonsi Julian", "04/28", "290");

        assertThat(card.getLastFourDigits()).isEqualTo("3486");
        assertThat(card.cardHolderName()).isEqualTo("Fonsi Julian");
        assertThat(card.cardExpirationDate()).isEqualTo("04/28");
    }

    @Test
    @DisplayName("PCI-DSS: Should mask card number and hide CVV in toString()")
    void shouldMaskSensitiveDataInToString() {
        final CardDetails card = new CardDetails("4532123456783486", "Fonsi Julian", "04/28", "290");
        final String toStringResult = card.toString();

        assertThat(toStringResult).contains("cardNumber=****3486", "cardCvv=***");
        assertThat(toStringResult).doesNotContain("4532123456783486");
        assertThat(toStringResult).doesNotContain("290");
        assertThat(card.cardCvv()).isEqualTo("290");
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "12", "1"})
    @DisplayName("Should throw CardValidationException when card number has fewer than 4 digits")
    void shouldThrowExceptionForShortCardNumber(final String shortCardNumber) {
        assertThatThrownBy(() -> new CardDetails(shortCardNumber, "Test Holder", "04/28", "123"))
                .isInstanceOf(CardValidationException.class)
                .hasMessageContaining("Card number must contain at least 4 digits");
    }

    @Test
    @DisplayName("Edge Case: Should throw CardValidationException when card number is null")
    void shouldThrowExceptionForNullCardNumber() {
        assertThatThrownBy(() -> new CardDetails(null, "Test Holder", "04/28", "123"))
                .isInstanceOf(CardValidationException.class)
                .hasMessageContaining("Card number cannot be empty");
    }

    @Test
    @DisplayName("Should throw CardValidationException when card number is blank")
    void shouldThrowExceptionForBlankCardNumber() {
        assertThatThrownBy(() -> new CardDetails("", "Test Holder", "04/28", "123"))
                .isInstanceOf(CardValidationException.class)
                .hasMessageContaining("Card number cannot be empty");
    }

    @Test
    @DisplayName("Should throw CardValidationException when card holder name is blank or null")
    void shouldThrowExceptionForBlankHolderName() {
        assertThatThrownBy(() -> new CardDetails("4532123456783486", "", "04/28", "123"))
                .isInstanceOf(CardValidationException.class)
                .hasMessageContaining("Card holder name cannot be empty");

        assertThatThrownBy(() -> new CardDetails("4532123456783486", null, "04/28", "123"))
                .isInstanceOf(CardValidationException.class)
                .hasMessageContaining("Card holder name cannot be empty");
    }

    @Test
    @DisplayName("Should throw CardValidationException when card expiration date is blank or null")
    void shouldThrowExceptionForBlankExpirationDate() {
        assertThatThrownBy(() -> new CardDetails("4532123456783486", "Test Holder", "", "123"))
                .isInstanceOf(CardValidationException.class)
                .hasMessageContaining("Card expiration date cannot be empty");

        assertThatThrownBy(() -> new CardDetails("4532123456783486", "Test Holder", null, "123"))
                .isInstanceOf(CardValidationException.class)
                .hasMessageContaining("Card expiration date cannot be empty");
    }

    @ParameterizedTest
    @ValueSource(strings = {"12", "12345", "abc", ""})
    @DisplayName("Should throw CardValidationException for invalid CVV format")
    void shouldThrowExceptionForInvalidCvv(final String invalidCvv) {
        assertThatThrownBy(() -> new CardDetails("4532123456783486", "Test Holder", "04/28", invalidCvv))
                .isInstanceOf(CardValidationException.class)
                .hasMessageContaining("CVV must be 3 or 4 numeric digits");
    }

    @Test
    @DisplayName("Edge Case: Should throw CardValidationException when CVV is null")
    void shouldThrowExceptionForNullCvv() {
        assertThatThrownBy(() -> new CardDetails("4532123456783486", "Test Holder", "04/28", null))
                .isInstanceOf(CardValidationException.class)
                .hasMessageContaining("CVV must be 3 or 4 numeric digits");
    }
}
