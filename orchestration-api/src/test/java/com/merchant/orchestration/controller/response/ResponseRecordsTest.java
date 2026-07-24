package com.merchant.orchestration.controller.response;

import com.merchant.orchestration.domain.CardDetails;
import com.merchant.orchestration.domain.PaymentMethod;
import com.merchant.orchestration.domain.Receivable;
import com.merchant.orchestration.domain.Transaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseRecordsTest {

    @Test
    @DisplayName("Should test TransactionResponse record accessors, equals, hashCode, toString, and fromDomain")
    void shouldTestTransactionResponse() {
        final CardDetails cardDetails = new CardDetails("4532123456783486", "Fonsi Julian", "04/28", "290");
        final Transaction domainTx = new Transaction("100", new BigDecimal("340.50"), "T-Shirt", PaymentMethod.DEBIT_CARD, cardDetails);

        final TransactionResponse response1 = TransactionResponse.fromDomain(domainTx);
        final TransactionResponse response2 = new TransactionResponse(
                "100", "340.50", "T-Shirt", "debit_card", "3486", "Fonsi Julian", "04/28"
        );

        assertThat(response1.id()).isEqualTo("100");
        assertThat(response1.value()).isEqualTo("340.50");
        assertThat(response1.description()).isEqualTo("T-Shirt");
        assertThat(response1.method()).isEqualTo("debit_card");
        assertThat(response1.cardNumber()).isEqualTo("3486");
        assertThat(response1.cardHolderName()).isEqualTo("Fonsi Julian");
        assertThat(response1.cardExpirationDate()).isEqualTo("04/28");

        assertThat(response1).isEqualTo(response2);
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
        assertThat(response1.toString()).contains("100");
    }

    @Test
    @DisplayName("Should test ReceivableResponse record accessors, equals, hashCode, toString, and fromDomain")
    void shouldTestReceivableResponse() {
        final Receivable domainReceivable = new Receivable("200", "100", "paid", "2026-07-21", "340.50", "6.81", "333.69");

        final ReceivableResponse response1 = ReceivableResponse.fromDomain(domainReceivable);
        final ReceivableResponse response2 = new ReceivableResponse(
                "200", "100", "paid", "2026-07-21", "340.50", "6.81", "333.69"
        );

        assertThat(response1.id()).isEqualTo("200");
        assertThat(response1.transactionId()).isEqualTo("100");
        assertThat(response1.status()).isEqualTo("paid");
        assertThat(response1.createDate()).isEqualTo("2026-07-21");
        assertThat(response1.subtotal()).isEqualTo("340.50");
        assertThat(response1.discount()).isEqualTo("6.81");
        assertThat(response1.total()).isEqualTo("333.69");

        assertThat(response1).isEqualTo(response2);
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
        assertThat(response1.toString()).contains("200");
    }

    @Test
    @DisplayName("Should test OrchestrationResponse record accessors, equals, hashCode, and toString")
    void shouldTestOrchestrationResponse() {
        final TransactionResponse transactionResponse = new TransactionResponse(
                "100", "340.50", "T-Shirt", "debit_card", "3486", "Fonsi Julian", "04/28"
        );
        final ReceivableResponse receivableResponse = new ReceivableResponse(
                "200", "100", "paid", "2026-07-21", "340.50", "6.81", "333.69"
        );
        final OrchestrationResponse response1 = new OrchestrationResponse(transactionResponse, receivableResponse);
        final OrchestrationResponse response2 = new OrchestrationResponse(transactionResponse, receivableResponse);

        assertThat(response1.transaction()).isEqualTo(transactionResponse);
        assertThat(response1.receivable()).isEqualTo(receivableResponse);

        assertThat(response1).isEqualTo(response2);
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
        assertThat(response1.toString()).contains("100");
    }
}
