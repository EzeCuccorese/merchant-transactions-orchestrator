package com.merchant.orchestration.domain;

import lombok.Getter;

/**
 * Enumeration representing standardized operational audit alert codes for logging and monitoring.
 */
@Getter
public enum AuditAlertCode {
    CRITICAL_AUDIT_ALERT("CRITICAL_AUDIT_ALERT", "Compensating transaction rollback failed after max retries. Manual DB reconciliation required."),
    NUMERATOR_CAS_CONFLICT("NUMERATOR_CAS_CONFLICT", "Numerator CAS conflict encountered during unique ID allocation."),
    ORCHESTRATION_ROLLBACK("ORCHESTRATION_ROLLBACK", "Compensating transaction rollback initiated due to receivable persistence failure.");

    private final String code;
    private final String description;

    AuditAlertCode(final String code, final String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public String toString() {
        return code;
    }
}
