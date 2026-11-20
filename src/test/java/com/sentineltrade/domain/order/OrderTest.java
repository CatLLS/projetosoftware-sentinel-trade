package com.sentineltrade.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class OrderTest {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private static LimitOrder newOrder() {
        return new LimitOrder("key-0001", 1L, "PETR4", OrderSide.BUY, HUNDRED, new BigDecimal("30.00"));
    }

    private static LimitOrder openOrder() {
        LimitOrder order = newOrder();
        order.validate();
        order.markSent();
        order.accept();
        return order;
    }

    @Test
    void newOrderStartsCreatedAndExposesMetadata() {
        LimitOrder order = newOrder();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.getType()).isEqualTo(OrderType.LIMIT);
        assertThat(order.getIdempotencyKey()).isEqualTo("key-0001");
        assertThat(order.getAccountId()).isEqualTo(1L);
        assertThat(order.getAssetSymbol()).isEqualTo("PETR4");
        assertThat(order.getSide()).isEqualTo(OrderSide.BUY);
        assertThat(order.getQuantity()).isEqualByComparingTo("100");
        assertThat(order.getExecutedQuantity()).isEqualByComparingTo("0");
        assertThat(order.getRemainingQuantity()).isEqualByComparingTo("100");
        assertThat(order.getPrice()).isEqualByComparingTo("30.00");
        assertThat(order.getCreatedAt()).isNotNull();
        assertThat(order.getUpdatedAt()).isNotNull();
    }

    @Test
    void marketOrderReportsMarketTypeAndReferencePrice() {
        MarketOrder order = new MarketOrder("key-mkt", 2L, "VALE3", OrderSide.SELL,
                new BigDecimal("50"), new BigDecimal("61.20"));

        assertThat(order.getType()).isEqualTo(OrderType.MARKET);
        assertThat(order.getSide()).isEqualTo(OrderSide.SELL);
        assertThat(order.getNotional()).isEqualByComparingTo("3060.0000");
    }

    @Test
    void constructorRejectsNonPositiveQuantityOrPrice() {
        assertThatThrownBy(() -> new LimitOrder("k", 1L, "PETR4", OrderSide.BUY, null, BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LimitOrder("k", 1L, "PETR4", OrderSide.BUY, BigDecimal.ZERO, BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LimitOrder("k", 1L, "PETR4", OrderSide.BUY, new BigDecimal("-1"), BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LimitOrder("k", 1L, "PETR4", OrderSide.BUY, HUNDRED, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LimitOrder("k", 1L, "PETR4", OrderSide.BUY, HUNDRED, new BigDecimal("-5")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void happyPathWithPartialExecution() {
        LimitOrder order = openOrder();

        order.registerExecution(new BigDecimal("40"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PARTIALLY_EXECUTED);
        assertThat(order.getRemainingQuantity()).isEqualByComparingTo("60");

        order.registerExecution(new BigDecimal("20"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PARTIALLY_EXECUTED);
        assertThat(order.getRemainingQuantity()).isEqualByComparingTo("40");

        order.registerExecution(new BigDecimal("40"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.EXECUTED);
        assertThat(order.getStatus().isFinal()).isTrue();
    }

    @Test
    void validationFailureRejectsWithReason() {
        LimitOrder order = newOrder();
        order.reject("Insufficient available balance");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(order.getRejectionReason()).isEqualTo("Insufficient available balance");
    }

    @Test
    void sentOrderCanBeRejectedDirectlyByExchange() {
        LimitOrder order = newOrder();
        order.validate();
        order.markSent();
        order.reject("Outside price band");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(order.getRejectionReason()).isEqualTo("Outside price band");
    }

    @Test
    void timeoutGoesToReconciliationAndCanBeResolvedToAllAllowedStates() {
        LimitOrder toOpen = newOrder();
        toOpen.validate();
        toOpen.markSent();
        toOpen.markPendingReconciliation();
        assertThat(toOpen.getStatus()).isEqualTo(OrderStatus.PENDING_RECONCILIATION);
        toOpen.accept();
        assertThat(toOpen.getStatus()).isEqualTo(OrderStatus.OPEN);

        LimitOrder toRejected = newOrder();
        toRejected.validate();
        toRejected.markSent();
        toRejected.markPendingReconciliation();
        toRejected.reject("Order unknown to the exchange");
        assertThat(toRejected.getStatus()).isEqualTo(OrderStatus.REJECTED);

        LimitOrder toExecuted = newOrder();
        toExecuted.validate();
        toExecuted.markSent();
        toExecuted.markPendingReconciliation();
        toExecuted.registerExecution(HUNDRED);
        assertThat(toExecuted.getStatus()).isEqualTo(OrderStatus.EXECUTED);
    }

    @Test
    void cancellationNeedsConfirmationFromOpenOrPartiallyExecuted() {
        LimitOrder order = openOrder();
        order.registerExecution(new BigDecimal("25"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PARTIALLY_EXECUTED);

        order.requestCancel();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCEL_REQUESTED);

        order.confirmCancel();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void executionWinsTheRaceAgainstCancellation() {
        LimitOrder order = openOrder();
        order.requestCancel();
        order.registerExecution(HUNDRED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.EXECUTED);
    }

    @Test
    void partialExecutionWhileCancellingKeepsWaitingForConfirmation() {
        LimitOrder order = openOrder();
        order.requestCancel();
        order.registerExecution(new BigDecimal("10"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCEL_REQUESTED);
        assertThat(order.getExecutedQuantity()).isEqualByComparingTo("10");
    }

    @Test
    void openAndPartiallyExecutedOrdersCanExpire() {
        LimitOrder open = openOrder();
        open.expire();
        assertThat(open.getStatus()).isEqualTo(OrderStatus.EXPIRED);

        LimitOrder partial = openOrder();
        partial.registerExecution(new BigDecimal("10"));
        partial.expire();
        assertThat(partial.getStatus()).isEqualTo(OrderStatus.EXPIRED);
    }

    @Test
    void cannotBeSentWithoutValidation() {
        assertThatThrownBy(() -> newOrder().markSent()).isInstanceOf(InvalidOrderStateException.class);
    }

    @Test
    void cannotBeCancelledBeforeTheExchangeAcceptsIt() {
        LimitOrder order = newOrder();
        order.validate();
        assertThatThrownBy(order::requestCancel).isInstanceOf(InvalidOrderStateException.class);
    }

    @Test
    void finalStatesAcceptNoTransition() {
        LimitOrder order = openOrder();
        order.registerExecution(HUNDRED);

        List<Consumer<Order>> transitions = List.of(Order::validate, Order::markSent, Order::accept,
                Order::markPendingReconciliation, Order::requestCancel, Order::confirmCancel,
                Order::expire, o -> o.reject("x"));
        transitions.forEach(transition -> assertThatThrownBy(() -> transition.accept(order))
                .isInstanceOf(InvalidOrderStateException.class));
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"EXECUTED", "REJECTED", "CANCELLED", "EXPIRED"})
    void finalStatusesHaveNoSuccessor(OrderStatus status) {
        assertThat(status.isFinal()).isTrue();
        for (OrderStatus target : OrderStatus.values()) {
            assertThat(status.canTransitionTo(target)).isFalse();
        }
    }

    @Test
    void cannotExecuteZeroNegativeOrMoreThanRemainingQuantity() {
        LimitOrder order = openOrder();
        assertThatThrownBy(() -> order.registerExecution(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> order.registerExecution(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> order.registerExecution(new BigDecimal("101")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void limitPriceCanOnlyChangeWhileOpenAndUntouchedWithPositivePrice() {
        LimitOrder created = newOrder();
        assertThatThrownBy(() -> created.changeLimitPrice(new BigDecimal("31.00")))
                .isInstanceOf(InvalidOrderStateException.class);

        LimitOrder order = openOrder();
        assertThatThrownBy(() -> order.changeLimitPrice(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> order.changeLimitPrice(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);

        order.changeLimitPrice(new BigDecimal("31.00"));
        assertThat(order.getPrice()).isEqualByComparingTo("31.00");

        order.registerExecution(BigDecimal.ONE);
        assertThatThrownBy(() -> order.changeLimitPrice(new BigDecimal("32.00")))
                .isInstanceOf(InvalidOrderStateException.class);
    }

    @Test
    void limitOrderRequiresPositivePrice() {
        assertThatThrownBy(() -> new LimitOrder("k", 1L, "PETR4", OrderSide.BUY, HUNDRED, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void notionalIsQuantityTimesPrice() {
        assertThat(newOrder().getNotional()).isEqualByComparingTo("3000.00");
    }
}
