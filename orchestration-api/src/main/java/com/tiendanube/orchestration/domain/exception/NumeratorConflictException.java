package com.tiendanube.orchestration.domain.exception;

/**
 * Exception thrown when ID generation fails after maximum retries due to numerator conflicts.
 */
public class NumeratorConflictException extends BusinessException {

    public NumeratorConflictException(final String message) {
        super(message, "NUMERATOR_CONFLICT");
    }

    public NumeratorConflictException(final String message, final Throwable cause) {
        super(message, "NUMERATOR_CONFLICT", cause);
    }
}
