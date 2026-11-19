package com.sentineltrade.domain.order;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.math.BigDecimal;

/** Executes immediately at the market price quoted when the order was placed. */
@Entity
@DiscriminatorValue("MARKET")
public class MarketOrder extends Order {

    protected MarketOrder() {
    }

    public MarketOrder(String idempotencyKey, Long accountId, String assetSymbol,
                       OrderSide side, BigDecimal quantity, BigDecimal referencePrice) {
        super(idempotencyKey, accountId, assetSymbol, side, quantity, referencePrice);
    }

    @Override
    public OrderType getType() {
        return OrderType.MARKET;
    }
}
