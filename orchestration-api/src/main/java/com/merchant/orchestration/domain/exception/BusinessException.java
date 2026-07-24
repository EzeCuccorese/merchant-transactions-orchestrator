package com.merchant.orchestration.domain.exception;

/**
 * Abstract base class for all business domain exceptions.
 */
public abstract class BusinessException extends RuntimeException {

    private final String errorCode;

    protected BusinessException(final String message, final String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    protected BusinessException(final String message, final String errorCode, final Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
