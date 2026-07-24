package com.merchant.orchestration.domain.exception;

/**
 * Exception thrown when a transaction ID is not found.
 */
public class TransactionNotFoundException extends BusinessException {

    public TransactionNotFoundException(final String id) {
        super("Transaction not found with ID: " + id, "TRANSACTION_NOT_FOUND");
    }
}
