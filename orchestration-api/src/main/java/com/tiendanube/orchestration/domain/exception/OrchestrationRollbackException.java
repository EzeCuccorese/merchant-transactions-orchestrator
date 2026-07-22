package com.tiendanube.orchestration.domain.exception;

/**
 * Exception thrown when a partial failure occurs during orchestration (e.g. receivable creation fails)
 * and compensating action (DELETE transaction) is triggered.
 */
public class OrchestrationRollbackException extends BusinessException {

    public OrchestrationRollbackException(final String message, final Throwable cause) {
        super(message, "ORCHESTRATION_ROLLBACK", cause);
    }
}
