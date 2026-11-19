package com.sentineltrade.domain.exchange;

import java.math.BigDecimal;

/**
 * What the exchange knows about an order.
 *
 * @param executedQuantity cumulative quantity executed so far
 * @param reason           why the order was rejected, when it was
 */
public record ExchangeReport(Status status, BigDecimal executedQuantity, String reason) {

    public enum Status {
        /** Live or fully executed; see {@code executedQuantity}. */
        ACCEPTED,
        REJECTED,
        CANCELLED,
        EXPIRED,
        NOT_FOUND
    }
}
