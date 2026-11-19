package com.sentineltrade.domain.account;

import com.sentineltrade.domain.order.Order;
import com.sentineltrade.domain.order.OrderSide;
import java.math.BigDecimal;

/**
 * Domain service that moves money and assets for an order, depending on its side:
 * a BUY works on the account balance, a SELL on the asset position.
 */
public final class Settlement {

    private Settlement() {
    }

    public static void reserve(Order order, Account account, Position position) {
        if (order.getSide() == OrderSide.BUY) {
            account.reserve(order.amountFor(order.getRemainingQuantity()));
        } else {
            position.reserve(order.getRemainingQuantity());
        }
    }

    public static void releaseRemaining(Order order, Account account, Position position) {
        if (order.getSide() == OrderSide.BUY) {
            account.release(order.amountFor(order.getRemainingQuantity()));
        } else {
            position.release(order.getRemainingQuantity());
        }
    }

    public static void settleFill(Order order, BigDecimal qty, Account account, Position position) {
        BigDecimal amount = order.amountFor(qty);
        if (order.getSide() == OrderSide.BUY) {
            account.settlePurchase(amount);
            position.addPurchase(qty, order.getPrice());
        } else {
            position.removeSale(qty);
            account.credit(amount);
        }
    }
}
