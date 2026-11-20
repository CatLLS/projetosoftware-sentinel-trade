package com.sentineltrade.domain.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sentineltrade.domain.risk.InsufficientPositionException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PositionTest {

    @Test
    void newPositionStartsEmpty() {
        Position position = new Position(10L, "PETR4");

        assertThat(position.getAccountId()).isEqualTo(10L);
        assertThat(position.getAssetSymbol()).isEqualTo("PETR4");
        assertThat(position.getQuantity()).isEqualByComparingTo("0");
        assertThat(position.getReservedQuantity()).isEqualByComparingTo("0");
        assertThat(position.getFreeQuantity()).isEqualByComparingTo("0");
        assertThat(position.getAveragePrice()).isEqualByComparingTo("0");
    }

    @Test
    void addPurchaseUpdatesQuantityAndWeightedAveragePrice() {
        Position position = new Position(10L, "PETR4");

        position.addPurchase(new BigDecimal("100"), new BigDecimal("30.00"));
        assertThat(position.getQuantity()).isEqualByComparingTo("100");
        assertThat(position.getAveragePrice()).isEqualByComparingTo("30.0000");

        // 100 @ 30.00 + 100 @ 40.00 = 200 @ 35.00
        position.addPurchase(new BigDecimal("100"), new BigDecimal("40.00"));
        assertThat(position.getQuantity()).isEqualByComparingTo("200");
        assertThat(position.getAveragePrice()).isEqualByComparingTo("35.0000");
    }

    @Test
    void reserveAndReleaseUpdateFreeQuantity() {
        Position position = new Position(10L, "VALE3");
        position.addPurchase(new BigDecimal("50"), new BigDecimal("60.00"));

        position.reserve(new BigDecimal("20"));
        assertThat(position.getReservedQuantity()).isEqualByComparingTo("20");
        assertThat(position.getFreeQuantity()).isEqualByComparingTo("30");

        position.release(new BigDecimal("15"));
        assertThat(position.getReservedQuantity()).isEqualByComparingTo("5");
        assertThat(position.getFreeQuantity()).isEqualByComparingTo("45");
    }

    @Test
    void reserveAboveFreeQuantityThrowsInsufficientPosition() {
        Position position = new Position(10L, "VALE3");
        position.addPurchase(new BigDecimal("50"), new BigDecimal("60.00"));
        position.reserve(new BigDecimal("30"));

        assertThatThrownBy(() -> position.reserve(new BigDecimal("21")))
                .isInstanceOf(InsufficientPositionException.class);
    }

    @Test
    void releaseAboveReservedQuantityThrowsIllegalState() {
        Position position = new Position(10L, "VALE3");
        position.addPurchase(new BigDecimal("50"), new BigDecimal("60.00"));
        position.reserve(new BigDecimal("10"));

        assertThatThrownBy(() -> position.release(new BigDecimal("11")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Reserved quantity would become negative");
    }

    @Test
    void removeSaleConsumesReservedAndReducesTotalQuantity() {
        Position position = new Position(10L, "VALE3");
        position.addPurchase(new BigDecimal("50"), new BigDecimal("60.00"));
        position.reserve(new BigDecimal("20"));

        position.removeSale(new BigDecimal("20"));

        assertThat(position.getReservedQuantity()).isEqualByComparingTo("0");
        assertThat(position.getQuantity()).isEqualByComparingTo("30");
        assertThat(position.getFreeQuantity()).isEqualByComparingTo("30");
    }
}
