package com.merchant.orchestration.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Domain entity representing a merchant receivable with calculated fee discount.
 */
public record Receivable(
        @JsonProperty("id") String id,
        @JsonProperty("transaction_id") String transactionId,
        @JsonProperty("status") String status,
        @JsonProperty("create_date") String createDate,
        @JsonProperty("subtotal") String subtotal,
        @JsonProperty("discount") String discount,
        @JsonProperty("total") String total
) {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * Domain factory method creating a Receivable from a Transaction and PaymentMethod.
     *
     * @param id           unique receivable ID from numerator
     * @param transactionId associated transaction ID
     * @param amount       subtotal transaction amount
     * @param method       payment method domain enum
     * @param creationDate transaction creation date
     * @return initialized Receivable entity
     */
    public static Receivable fromTransaction(
            final String id,
            final String transactionId,
            final BigDecimal amount,
            final PaymentMethod method,
            final LocalDate creationDate
    ) {
        final BigDecimal feeDiscount = method.calculateFee(amount);
        final BigDecimal netTotal = method.calculateNetTotal(amount);
        final LocalDate paymentDate = method.calculatePaymentDate(creationDate);

        return new Receivable(
                id,
                transactionId,
                method.getStatus(),
                paymentDate.format(DATE_FORMATTER),
                amount.toPlainString(),
                feeDiscount.toPlainString(),
                netTotal.toPlainString()
        );
    }
}
