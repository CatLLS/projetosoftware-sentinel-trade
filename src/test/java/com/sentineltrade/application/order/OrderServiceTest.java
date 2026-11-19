package com.sentineltrade.application.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sentineltrade.application.audit.AuditEvent;
import com.sentineltrade.domain.account.Account;
import com.sentineltrade.domain.exchange.ExchangeGateway;
import com.sentineltrade.domain.exchange.ExchangeTimeoutException;
import com.sentineltrade.domain.market.MarketQuoteProvider;
import com.sentineltrade.domain.order.DuplicateOrderException;
import com.sentineltrade.domain.order.LimitOrder;
import com.sentineltrade.domain.order.Order;
import com.sentineltrade.domain.order.OrderSide;
import com.sentineltrade.domain.order.OrderStatus;
import com.sentineltrade.domain.order.OrderType;
import com.sentineltrade.domain.risk.AssetPositionRule;
import com.sentineltrade.domain.risk.BalanceSufficiencyRule;
import com.sentineltrade.domain.risk.InsufficientBalanceException;
import com.sentineltrade.domain.risk.OrderExposureLimitRule;
import com.sentineltrade.infrastructure.persistence.AccountRepository;
import com.sentineltrade.infrastructure.persistence.OrderRepository;
import com.sentineltrade.infrastructure.persistence.PositionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/** Idempotency and failure handling of order placement, with every port mocked. */
class OrderServiceTest {

    private static final long USER_ID = 7L;
    private static final long ACCOUNT_ID = 10L;
    private static final String KEY = "key-0001";

    private final OrderRepository orders = mock(OrderRepository.class);
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final PositionRepository positions = mock(PositionRepository.class);
    private final MarketQuoteProvider quotes = mock(MarketQuoteProvider.class);
    private final ExchangeGateway exchange = mock(ExchangeGateway.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final TransactionTemplate tx = mock(TransactionTemplate.class);

    private final Account account = new Account(USER_ID, new BigDecimal("10000.00"), new BigDecimal("50000.00"));
    private OrderService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(account, "id", ACCOUNT_ID);
        when(accounts.findByUserId(USER_ID)).thenReturn(Optional.of(account));
        when(accounts.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(positions.findByAccountIdAndAssetSymbol(anyLong(), anyString())).thenReturn(Optional.empty());
        when(quotes.currentPrice("PETR4")).thenReturn(new BigDecimal("37.50"));
        when(tx.execute(any())).thenAnswer(inv -> inv.<TransactionCallback<?>>getArgument(0).doInTransaction(null));

        service = new OrderService(orders, accounts, positions,
                List.of(new BalanceSufficiencyRule(), new AssetPositionRule(), new OrderExposureLimitRule()),
                quotes, exchange, events, tx);
    }

    private static PlaceOrderCommand limitBuy(String quantity, String price) {
        return new PlaceOrderCommand(KEY, OrderType.LIMIT, "PETR4", OrderSide.BUY,
                new BigDecimal(quantity), new BigDecimal(price));
    }

    /** Makes save() behave like the database: assigns an id and makes the order findable. */
    private void persistOrdersInMemory() {
        when(orders.save(any())).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            ReflectionTestUtils.setField(order, "id", 1L);
            when(orders.findById(1L)).thenReturn(Optional.of(order));
            return order;
        });
    }

    @Test
    void replayWithSamePayloadReturnsTheExistingOrder() {
        Order existing = limitBuy("10", "36.00").toOrder(ACCOUNT_ID, null);
        when(orders.findByIdempotencyKey(KEY)).thenReturn(Optional.of(existing));

        OrderService.Placement placement = service.place(USER_ID, limitBuy("10", "36.00"));

        assertThat(placement.created()).isFalse();
        assertThat(placement.order()).isSameAs(existing);
        verify(exchange, never()).submit(any());
        verify(events).publishEvent(any(AuditEvent.class));
    }

    @Test
    void replayWithDifferentPayloadIsBlocked() {
        Order existing = limitBuy("10", "36.00").toOrder(ACCOUNT_ID, null);
        when(orders.findByIdempotencyKey(KEY)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.place(USER_ID, limitBuy("20", "36.00")))
                .isInstanceOf(DuplicateOrderException.class);
        ArgumentCaptor<AuditEvent> audit = ArgumentCaptor.forClass(AuditEvent.class);
        verify(events).publishEvent(audit.capture());
        assertThat(audit.getValue().type()).isEqualTo("ORDER_DUPLICATE_BLOCKED");
    }

    @Test
    void keyUsedByAnotherAccountIsBlocked() {
        Order othersOrder = new LimitOrder(KEY, 99L, "PETR4", OrderSide.BUY, BigDecimal.TEN, new BigDecimal("36.00"));
        when(orders.findByIdempotencyKey(KEY)).thenReturn(Optional.of(othersOrder));

        assertThatThrownBy(() -> service.place(USER_ID, limitBuy("10", "36.00")))
                .isInstanceOf(DuplicateOrderException.class);
    }

    @Test
    void ruleViolationPersistsARejectedOrderWithoutReservation() {
        persistOrdersInMemory();
        when(orders.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.place(USER_ID, limitBuy("1000", "36.00"))) // 36,000 > 10,000
                .isInstanceOf(InsufficientBalanceException.class);

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orders).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(account.getReservedBalance()).isEqualByComparingTo("0");
        verify(exchange, never()).submit(any());
    }

    @Test
    void exchangeTimeoutLeadsToReconciliationWithoutResubmitting() {
        persistOrdersInMemory();
        when(orders.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(exchange.submit(any())).thenThrow(new ExchangeTimeoutException());

        Order order = service.place(USER_ID, limitBuy("100", "36.00")).order();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_RECONCILIATION);
        assertThat(account.getReservedBalance()).isEqualByComparingTo("3600.00"); // still reserved
        verify(exchange, times(1)).submit(any());
    }
}
