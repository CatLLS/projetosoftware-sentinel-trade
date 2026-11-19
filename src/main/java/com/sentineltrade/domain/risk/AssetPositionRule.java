package com.sentineltrade.domain.risk;

import com.sentineltrade.domain.order.Order;
import com.sentineltrade.domain.order.OrderSide;
import org.springframework.stereotype.Component;

/** A sell order must be covered by the free (non-reserved) quantity of the asset. */
@Component
public class AssetPositionRule implements RiskRule {

    @Override
    public void check(Order order, RiskContext context) {
        if (order.getSide() != OrderSide.SELL) {
            return;
        }
        if (context.position() == null
                || context.position().getFreeQuantity().compareTo(order.getQuantity()) < 0) {
            throw new InsufficientPositionException();
        }
    }
}
