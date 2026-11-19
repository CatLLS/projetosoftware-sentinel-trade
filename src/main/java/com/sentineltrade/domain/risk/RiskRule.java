package com.sentineltrade.domain.risk;

import com.sentineltrade.domain.order.Order;

/** Strategy for a single pre-trade check. Throws a {@link RiskViolationException} on failure. */
public interface RiskRule {

    void check(Order order, RiskContext context);
}
