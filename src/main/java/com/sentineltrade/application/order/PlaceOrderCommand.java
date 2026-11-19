package com.sentineltrade.application.order;

import com.sentineltrade.domain.order.LimitOrder;
import com.sentineltrade.domain.order.MarketOrder;
import com.sentineltrade.domain.order.Order;
import com.sentineltrade.domain.order.OrderSide;
import com.sentineltrade.domain.order.OrderType;
import java.math.BigDecimal;

/** @param limitPrice required for LIMIT orders, ignored for MARKET orders */
public record PlaceOrderCommand(String idempotencyKey, OrderType type, String assetSymbol,
                                OrderSide side, BigDecimal quantity, BigDecimal limitPrice) {

    public Order toOrder(Long accountId, BigDecimal marketPrice) {
        return switch (type) {
            case MARKET -> new MarketOrder(idempotencyKey, accountId, assetSymbol, side, quantity, marketPrice);
            case LIMIT -> new LimitOrder(idempotencyKey, accountId, assetSymbol, side, quantity, limitPrice);
        };
    }

    /** Whether a replay with the same idempotency key carries the same request. */
    public boolean matches(Order order) {
        return order.getType() == type
                && order.getSide() == side
                && order.getAssetSymbol().equals(assetSymbol)
                && order.getQuantity().compareTo(quantity) == 0
                && (type == OrderType.MARKET || order.getPrice().compareTo(limitPrice) == 0);
    }
}
