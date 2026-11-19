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
    void newOrderStartsCreated() {
        assertThat(newOrder().getStatus()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void happyPathWithPartialExecution() {
        LimitOrder order = openOrder();

        order.registerExecution(new BigDecimal("40"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PARTIALLY_EXECUTED);
        assertThat(order.getRemainingQuantity()).isEqualByComparingTo("60");

        order.registerExecution(new BigDecimal("60"));
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
    void timeoutGoesToReconciliationAndCanBeResolved() {
        LimitOrder order = newOrder();
        order.validate();
        order.markSent();
        order.markPendingReconciliation();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_RECONCILIATION);

        order.accept();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.OPEN);
    }

    @Test
    void cancellationNeedsConfirmation() {
        LimitOrder order = openOrder();
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
    void openOrderCanExpire() {
        LimitOrder order = openOrder();
        order.expire();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.EXPIRED);
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
                Order::requestCancel, Order::confirmCancel, Order::expire, o -> o.reject("x"));
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
    void cannotExecuteMoreThanTheRemainingQuantity() {
        LimitOrder order = openOrder();
        assertThatThrownBy(() -> order.registerExecution(new BigDecimal("101")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void limitPriceCanOnlyChangeWhileOpenAndUntouched() {
        LimitOrder order = openOrder();
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
