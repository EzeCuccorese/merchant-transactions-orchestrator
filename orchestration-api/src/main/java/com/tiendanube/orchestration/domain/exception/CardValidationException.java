package com.tiendanube.orchestration.domain.exception;

/**
 * Exception thrown when card details fail PCI-DSS or domain validation rules.
 */
public class CardValidationException extends BusinessException {

    public CardValidationException(final String message) {
        super(message, "CARD_VALIDATION_ERROR");
    }
}
