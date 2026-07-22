package com.tiendanube.orchestration.domain.exception;

/**
 * Exception thrown when an unsupported or invalid payment method code is provided.
 */
public class InvalidPaymentMethodException extends BusinessException {

    public InvalidPaymentMethodException(final String methodCode) {
        super("Unsupported payment method: " + methodCode, "INVALID_PAYMENT_METHOD");
    }
}
