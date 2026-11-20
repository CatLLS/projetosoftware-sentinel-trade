package com.sentineltrade.domain.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.sentineltrade.domain.order.LimitOrder;
import com.sentineltrade.domain.order.OrderSide;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SettlementTest {

    @Test
    void buyOrderReservesSettlesAndReleasesRemainingBalance() {
        Account account = new Account(1L, new BigDecimal("5000.00"), new BigDecimal("50000.00"));
        Position position = new Position(1L, "PETR4");
        LimitOrder buy = new LimitOrder("k-buy", 1L, "PETR4", OrderSide.BUY,
                new BigDecimal("100"), new BigDecimal("30.00"));
        buy.validate();
        buy.markSent();
        buy.accept();

        Settlement.reserve(buy, account, position);
        assertThat(account.getAvailableBalance()).isEqualByComparingTo("2000.00");
        assertThat(account.getReservedBalance()).isEqualByComparingTo("3000.00");

        // Partial fill of 40 shares (1200.00)
        Settlement.settleFill(buy, new BigDecimal("40"), account, position);
        buy.registerExecution(new BigDecimal("40"));
        assertThat(account.getReservedBalance()).isEqualByComparingTo("1800.00");
        assertThat(position.getQuantity()).isEqualByComparingTo("40");
        assertThat(position.getAveragePrice()).isEqualByComparingTo("30.0000");

        // Cancel remaining 60 shares (1800.00)
        Settlement.releaseRemaining(buy, account, position);
        assertThat(account.getReservedBalance()).isEqualByComparingTo("0.00");
        assertThat(account.getAvailableBalance()).isEqualByComparingTo("3800.00");
    }

    @Test
    void sellOrderReservesSettlesAndReleasesRemainingPosition() {
        Account account = new Account(1L, new BigDecimal("1000.00"), new BigDecimal("50000.00"));
        Position position = new Position(1L, "PETR4");
        position.addPurchase(new BigDecimal("100"), new BigDecimal("25.00"));
        LimitOrder sell = new LimitOrder("k-sell", 1L, "PETR4", OrderSide.SELL,
                new BigDecimal("100"), new BigDecimal("35.00"));
        sell.validate();
        sell.markSent();
        sell.accept();

        Settlement.reserve(sell, account, position);
        assertThat(position.getReservedQuantity()).isEqualByComparingTo("100");
        assertThat(position.getFreeQuantity()).isEqualByComparingTo("0");

        // Partial fill of 30 shares @ 35.00 = 1050.00 credited
        Settlement.settleFill(sell, new BigDecimal("30"), account, position);
        sell.registerExecution(new BigDecimal("30"));
        assertThat(position.getQuantity()).isEqualByComparingTo("70");
        assertThat(position.getReservedQuantity()).isEqualByComparingTo("70");
        assertThat(account.getAvailableBalance()).isEqualByComparingTo("2050.00");

        // Release remaining 70 shares
        Settlement.releaseRemaining(sell, account, position);
        assertThat(position.getReservedQuantity()).isEqualByComparingTo("0");
        assertThat(position.getFreeQuantity()).isEqualByComparingTo("70");
    }
}
