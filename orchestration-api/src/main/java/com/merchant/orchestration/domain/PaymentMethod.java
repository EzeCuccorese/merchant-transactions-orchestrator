package com.merchant.orchestration.domain;

import com.merchant.orchestration.domain.exception.InvalidPaymentMethodException;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;

/**
 * Domain Enum representing supported payment methods with fee rates and settlement rules.
 */
@Getter
public enum PaymentMethod {

    DEBIT_CARD("debit_card", "paid", new BigDecimal("0.02"), 0),
    CREDIT_CARD("credit_card", "waiting_funds", new BigDecimal("0.04"), 30);

    private final String code;
    private final String status;
    private final BigDecimal feePercentage;
    private final long settlementDays;

    PaymentMethod(final String code, final String status, final BigDecimal feePercentage, final long settlementDays) {
        this.code = code;
        this.status = status;
        this.feePercentage = feePercentage;
        this.settlementDays = settlementDays;
    }

    /**
     * Calculates the fee amount for a given transaction total.
     *
     * @param total transaction total value
     * @return calculated fee rounded to 2 decimal places
     */
    public BigDecimal calculateFee(final BigDecimal total) {
        if (total == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return total.multiply(feePercentage).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the net total (total - fee) for the receivable.
     *
     * @param total transaction total value
     * @return net total receivable value
     */
    public BigDecimal calculateNetTotal(final BigDecimal total) {
        if (total == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        final BigDecimal fee = calculateFee(total);
        return total.subtract(fee).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the payment date offset from creation date.
     *
     * @param creationDate creation date
     * @return payment date
     */
    public LocalDate calculatePaymentDate(final LocalDate creationDate) {
        if (creationDate == null) {
            return LocalDate.now().plusDays(settlementDays);
        }
        return creationDate.plusDays(settlementDays);
    }

    /**
     * Factory method to obtain PaymentMethod from code string.
     *
     * @param code string code
     * @return matching PaymentMethod
     * @throws InvalidPaymentMethodException if code is invalid
     */
    public static PaymentMethod fromCode(final String code) {
        if (StringUtils.isBlank(code)) {
            throw new InvalidPaymentMethodException(code);
        }
        return Arrays.stream(values())
                .filter(method -> method.code.equalsIgnoreCase(code.trim()))
                .findFirst()
                .orElseThrow(() -> new InvalidPaymentMethodException(code));
    }
}
