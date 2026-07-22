package com.tiendanube.orchestration.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditAlertCodeTest {

    @Test
    @DisplayName("Should verify AuditAlertCode enum properties and toString output")
    void shouldVerifyAuditAlertCodeProperties() {
        final AuditAlertCode alert = AuditAlertCode.CRITICAL_AUDIT_ALERT;

        assertThat(alert.getCode()).isEqualTo("CRITICAL_AUDIT_ALERT");
        assertThat(alert.getDescription()).contains("Compensating transaction rollback failed");
        assertThat(alert.toString()).isEqualTo("CRITICAL_AUDIT_ALERT");
        assertThat(AuditAlertCode.valueOf("CRITICAL_AUDIT_ALERT")).isEqualTo(AuditAlertCode.CRITICAL_AUDIT_ALERT);
    }
}
