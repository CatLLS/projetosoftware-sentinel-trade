package com.sentineltrade.domain.order;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.math.BigDecimal;

/** Executes only at the limit price; its price can be changed while it is open and untouched. */
@Entity
@DiscriminatorValue("LIMIT")
public class LimitOrder extends Order {

    protected LimitOrder() {
    }

    public LimitOrder(String idempotencyKey, Long accountId, String assetSymbol,
                      OrderSide side, BigDecimal quantity, BigDecimal limitPrice) {
        super(idempotencyKey, accountId, assetSymbol, side, quantity, limitPrice);
    }

    @Override
    public OrderType getType() {
        return OrderType.LIMIT;
    }

    public void changeLimitPrice(BigDecimal newPrice) {
        if (newPrice == null || newPrice.signum() <= 0) {
            throw new IllegalArgumentException("Limit price must be positive");
        }
        if (getStatus() != OrderStatus.OPEN || getExecutedQuantity().signum() != 0) {
            throw new InvalidOrderStateException("Only open orders without executions can be amended");
        }
        reprice(newPrice);
    }
}
