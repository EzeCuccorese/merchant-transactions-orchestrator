package com.tiendanube.orchestration.domain;

import com.tiendanube.orchestration.domain.exception.CardValidationException;
import org.apache.commons.lang3.StringUtils;

/**
 * Value Object encapsulating card details, validation, and PCI-DSS card masking.
 */
public record CardDetails(
        String cardNumber,
        String cardHolderName,
        String cardExpirationDate,
        String cardCvv
) {

    public CardDetails {
        if (StringUtils.isBlank(cardNumber)) {
            throw new CardValidationException("Card number cannot be empty");
        }
        final String digitsOnly = cardNumber.replaceAll("\\D", "");
        if (digitsOnly.length() < 4) {
            throw new CardValidationException("Card number must contain at least 4 digits");
        }

        if (StringUtils.isBlank(cardHolderName)) {
            throw new CardValidationException("Card holder name cannot be empty");
        }

        if (StringUtils.isBlank(cardExpirationDate)) {
            throw new CardValidationException("Card expiration date cannot be empty");
        }

        if (StringUtils.isBlank(cardCvv) || !cardCvv.trim().matches("\\d{3,4}")) {
            throw new CardValidationException("CVV must be 3 or 4 numeric digits");
        }

        cardNumber = digitsOnly;
        cardHolderName = cardHolderName.trim();
        cardExpirationDate = cardExpirationDate.trim();
        cardCvv = cardCvv.trim();
    }

    /**
     * Extracts and returns strictly the last 4 digits of the card number for storage and display.
     *
     * @return last 4 digits
     */
    public String getLastFourDigits() {
        return cardNumber.substring(cardNumber.length() - 4);
    }

    @Override
    public String toString() {
        return "CardDetails[cardNumber=****" + getLastFourDigits() + ", cardHolderName=" + cardHolderName + ", cardExpirationDate=" + cardExpirationDate + ", cardCvv=***]";
    }
}
