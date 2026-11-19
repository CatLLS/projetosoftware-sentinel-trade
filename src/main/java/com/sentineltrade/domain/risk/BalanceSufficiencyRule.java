package com.sentineltrade.domain.risk;

import com.sentineltrade.domain.order.Order;
import com.sentineltrade.domain.order.OrderSide;
import org.springframework.stereotype.Component;

/** A buy order must be fully covered by the available balance. */
@Component
public class BalanceSufficiencyRule implements RiskRule {

    @Override
    public void check(Order order, RiskContext context) {
        if (order.getSide() == OrderSide.BUY
                && context.account().getAvailableBalance().compareTo(order.getNotional()) < 0) {
            throw new InsufficientBalanceException();
        }
    }
}
