package com.sentineltrade.domain.risk;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sentineltrade.domain.account.Account;
import com.sentineltrade.domain.account.Position;
import com.sentineltrade.domain.order.LimitOrder;
import com.sentineltrade.domain.order.Order;
import com.sentineltrade.domain.order.OrderSide;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RiskRulesTest {

    private final Account account = new Account(1L, new BigDecimal("1000.00"), new BigDecimal("5000.00"));

    private static Order order(OrderSide side, String quantity, String price) {
        return new LimitOrder("key-0001", 1L, "PETR4", side, new BigDecimal(quantity), new BigDecimal(price));
    }

    private static Position position(String quantity) {
        Position position = new Position(1L, "PETR4");
        position.addPurchase(new BigDecimal(quantity), new BigDecimal("10.00"));
        return position;
    }

    private RiskContext context(Position position, String tradedToday) {
        return new RiskContext(account, position, new BigDecimal(tradedToday));
    }

    // --- BalanceSufficiencyRule ----------------------------------------------

    @Test
    void buyCoveredByBalancePasses() {
        assertThatCode(() -> new BalanceSufficiencyRule().check(order(OrderSide.BUY, "100", "10.00"), context(null, "0")))
                .doesNotThrowAnyException();
    }

    @Test
    void buyAboveBalanceFails() {
        assertThatThrownBy(() -> new BalanceSufficiencyRule().check(order(OrderSide.BUY, "101", "10.00"), context(null, "0")))
                .isInstanceOf(InsufficientBalanceException.class);
    }

    @Test
    void balanceRuleIgnoresSells() {
        assertThatCode(() -> new BalanceSufficiencyRule().check(order(OrderSide.SELL, "1000", "10.00"), context(null, "0")))
                .doesNotThrowAnyException();
    }

    // --- AssetPositionRule ---------------------------------------------------

    @Test
    void sellCoveredByPositionPasses() {
        assertThatCode(() -> new AssetPositionRule().check(order(OrderSide.SELL, "100", "10.00"), context(position("100"), "0")))
                .doesNotThrowAnyException();
    }

    @Test
    void sellWithoutPositionFails() {
        assertThatThrownBy(() -> new AssetPositionRule().check(order(OrderSide.SELL, "1", "10.00"), context(null, "0")))
                .isInstanceOf(InsufficientPositionException.class);
    }

    @Test
    void sellOfReservedQuantityFails() {
        Position position = position("100");
        position.reserve(new BigDecimal("60"));

        assertThatThrownBy(() -> new AssetPositionRule().check(order(OrderSide.SELL, "50", "10.00"), context(position, "0")))
                .isInstanceOf(InsufficientPositionException.class);
    }

    // --- OrderExposureLimitRule ----------------------------------------------

    @Test
    void exposureWithinDailyLimitPasses() {
        assertThatCode(() -> new OrderExposureLimitRule().check(order(OrderSide.BUY, "50", "10.00"), context(null, "4500")))
                .doesNotThrowAnyException();
    }

    @Test
    void exposureAboveDailyLimitFails() {
        assertThatThrownBy(() -> new OrderExposureLimitRule().check(order(OrderSide.BUY, "51", "10.00"), context(null, "4500")))
                .isInstanceOf(RiskLimitExceededException.class);
    }
}
