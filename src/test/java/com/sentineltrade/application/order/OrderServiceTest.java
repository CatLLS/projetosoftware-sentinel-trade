package com.sentineltrade.application.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sentineltrade.application.ResourceNotFoundException;
import com.sentineltrade.application.audit.AuditEvent;
import com.sentineltrade.config.AppProperties;
import com.sentineltrade.domain.account.Account;
import com.sentineltrade.domain.account.Position;
import com.sentineltrade.domain.exchange.ExchangeGateway;
import com.sentineltrade.domain.exchange.ExchangeReport;
import com.sentineltrade.domain.exchange.ExchangeReport.Status;
import com.sentineltrade.domain.exchange.ExchangeTimeoutException;
import com.sentineltrade.domain.market.MarketQuoteProvider;
import com.sentineltrade.domain.order.DuplicateOrderException;
import com.sentineltrade.domain.order.InvalidOrderStateException;
import com.sentineltrade.domain.order.LimitOrder;
import com.sentineltrade.domain.order.MarketOrder;
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
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/** Comprehensive unit tests for {@link OrderService}, {@link PlaceOrderCommand}, and {@link OrderReconciler}. */
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

    private static PlaceOrderCommand marketBuy(String quantity) {
        return new PlaceOrderCommand(KEY, OrderType.MARKET, "PETR4", OrderSide.BUY,
                new BigDecimal(quantity), null);
    }

    private static PlaceOrderCommand marketSell(String quantity) {
        return new PlaceOrderCommand(KEY, OrderType.MARKET, "PETR4", OrderSide.SELL,
                new BigDecimal(quantity), null);
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

    // --- PlaceOrderCommand ---------------------------------------------------

    @Test
    void placeOrderCommandCreatesMarketAndLimitOrdersAndChecksPayloadMatch() {
        PlaceOrderCommand limitCmd = limitBuy("10", "36.00");
        Order limitOrder = limitCmd.toOrder(ACCOUNT_ID, new BigDecimal("37.50"));
        assertThat(limitOrder).isInstanceOf(LimitOrder.class);
        assertThat(limitCmd.matches(limitOrder)).isTrue();
        assertThat(limitBuy("20", "36.00").matches(limitOrder)).isFalse();
        assertThat(limitBuy("10", "35.00").matches(limitOrder)).isFalse();
        assertThat(new PlaceOrderCommand(KEY, OrderType.LIMIT, "VALE3", OrderSide.BUY, BigDecimal.TEN, new BigDecimal("36.00"))
                .matches(limitOrder)).isFalse();
        assertThat(new PlaceOrderCommand(KEY, OrderType.LIMIT, "PETR4", OrderSide.SELL, BigDecimal.TEN, new BigDecimal("36.00"))
                .matches(limitOrder)).isFalse();

        PlaceOrderCommand marketCmd = marketBuy("10");
        Order marketOrder = marketCmd.toOrder(ACCOUNT_ID, new BigDecimal("37.50"));
        assertThat(marketOrder).isInstanceOf(MarketOrder.class);
        assertThat(marketCmd.matches(marketOrder)).isTrue();
        assertThat(marketCmd.matches(limitOrder)).isFalse();
    }

    // --- Idempotency & Placement ---------------------------------------------

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
    void marketBuyExecutesImmediatelySettlesBalanceAndUpdatesPosition() {
        persistOrdersInMemory();
        when(orders.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(exchange.submit(any())).thenReturn(new ExchangeReport(Status.ACCEPTED, BigDecimal.TEN, null));

        OrderService.Placement placement = service.place(USER_ID, marketBuy("10"));

        assertThat(placement.created()).isTrue();
        assertThat(placement.order().getStatus()).isEqualTo(OrderStatus.EXECUTED);
        assertThat(placement.order().getExecutedQuantity()).isEqualByComparingTo("10");
        // 10 * 37.50 = 375.00 deducted from 10000.00
        assertThat(account.getAvailableBalance()).isEqualByComparingTo("9625.00");
        assertThat(account.getReservedBalance()).isEqualByComparingTo("0.00");
        ArgumentCaptor<Position> posCaptor = ArgumentCaptor.forClass(Position.class);
        verify(positions).save(posCaptor.capture());
        assertThat(posCaptor.getValue().getQuantity()).isEqualByComparingTo("10");
        assertThat(posCaptor.getValue().getAveragePrice()).isEqualByComparingTo("37.5000");
    }

    @Test
    void marketSellExecutesImmediatelyCreditsAccountAndReducesPosition() {
        persistOrdersInMemory();
        Position position = new Position(ACCOUNT_ID, "PETR4");
        position.addPurchase(new BigDecimal("100"), new BigDecimal("30.00"));
        when(positions.findByAccountIdAndAssetSymbol(ACCOUNT_ID, "PETR4")).thenReturn(Optional.of(position));
        when(orders.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(exchange.submit(any())).thenReturn(new ExchangeReport(Status.ACCEPTED, new BigDecimal("20"), null));

        Order order = service.place(USER_ID, marketSell("20")).order();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.EXECUTED);
        // 20 * 37.50 = 750.00 added to 10000.00
        assertThat(account.getAvailableBalance()).isEqualByComparingTo("10750.00");
        assertThat(position.getQuantity()).isEqualByComparingTo("80");
        assertThat(position.getReservedQuantity()).isEqualByComparingTo("0");
    }

    @Test
    void exchangeRejectionReleasesReservedFunds() {
        persistOrdersInMemory();
        when(orders.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(exchange.submit(any())).thenReturn(
                new ExchangeReport(Status.REJECTED, BigDecimal.ZERO, "Outside price band"));

        Order order = service.place(USER_ID, limitBuy("10", "36.00")).order();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(order.getRejectionReason()).isEqualTo("Outside price band");
        assertThat(account.getAvailableBalance()).isEqualByComparingTo("10000.00");
        assertThat(account.getReservedBalance()).isEqualByComparingTo("0.00");
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

    // --- Amend ---------------------------------------------------------------

    @Test
    void amendOpenLimitOrderAdjustsReservationAndNotifiesExchange() {
        persistOrdersInMemory();
        when(orders.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(exchange.submit(any())).thenReturn(new ExchangeReport(Status.ACCEPTED, BigDecimal.ZERO, null));
        Order order = service.place(USER_ID, limitBuy("100", "30.00")).order();
        assertThat(account.getReservedBalance()).isEqualByComparingTo("3000.00");

        when(exchange.amend(1L, new BigDecimal("32.00")))
                .thenReturn(new ExchangeReport(Status.ACCEPTED, BigDecimal.ZERO, null));

        Order amended = service.amend(USER_ID, 1L, new BigDecimal("32.00"));

        assertThat(amended.getPrice()).isEqualByComparingTo("32.00");
        assertThat(account.getReservedBalance()).isEqualByComparingTo("3200.00");
        assertThat(account.getAvailableBalance()).isEqualByComparingTo("6800.00");
    }

    @Test
    void amendRejectsMarketOrderOrNonOwner() {
        MarketOrder marketOrder = new MarketOrder(KEY, ACCOUNT_ID, "PETR4", OrderSide.BUY, BigDecimal.TEN, new BigDecimal("37.50"));
        ReflectionTestUtils.setField(marketOrder, "id", 2L);
        when(orders.findById(2L)).thenReturn(Optional.of(marketOrder));

        assertThatThrownBy(() -> service.amend(USER_ID, 2L, new BigDecimal("38.00")))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("Only limit orders can be amended");

        LimitOrder otherAccountOrder = new LimitOrder(KEY, 999L, "PETR4", OrderSide.BUY, BigDecimal.TEN, new BigDecimal("30.00"));
        ReflectionTestUtils.setField(otherAccountOrder, "id", 3L);
        when(orders.findById(3L)).thenReturn(Optional.of(otherAccountOrder));

        assertThatThrownBy(() -> service.amend(USER_ID, 3L, new BigDecimal("31.00")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- Cancel --------------------------------------------------------------

    @Test
    void cancelOpenOrderReleasesRemainingReservationWhenConfirmed() {
        persistOrdersInMemory();
        when(orders.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(exchange.submit(any())).thenReturn(new ExchangeReport(Status.ACCEPTED, new BigDecimal("40"), null));
        Order order = service.place(USER_ID, limitBuy("100", "30.00")).order();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PARTIALLY_EXECUTED);
        // 40 executed (1200 settled), 60 remaining reserved (1800)
        assertThat(account.getReservedBalance()).isEqualByComparingTo("1800.00");

        when(exchange.cancel(1L)).thenReturn(new ExchangeReport(Status.CANCELLED, new BigDecimal("40"), null));

        Order cancelled = service.cancel(USER_ID, 1L);

        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(account.getReservedBalance()).isEqualByComparingTo("0.00");
        assertThat(account.getAvailableBalance()).isEqualByComparingTo("8800.00");
    }

    @Test
    void cancelTimeoutLeavesOrderInCancelRequestedState() {
        persistOrdersInMemory();
        when(orders.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(exchange.submit(any())).thenReturn(new ExchangeReport(Status.ACCEPTED, BigDecimal.ZERO, null));
        service.place(USER_ID, limitBuy("100", "30.00"));

        when(exchange.cancel(1L)).thenThrow(new ExchangeTimeoutException());

        Order cancelling = service.cancel(USER_ID, 1L);

        assertThat(cancelling.getStatus()).isEqualTo(OrderStatus.CANCEL_REQUESTED);
        assertThat(account.getReservedBalance()).isEqualByComparingTo("3000.00");
    }

    // --- Reconcile & OrderReconciler -----------------------------------------

    @Test
    void reconcileResolvesPendingReconciliationToExecutedOrNotFoundOrExpired() {
        persistOrdersInMemory();
        when(orders.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(exchange.submit(any())).thenThrow(new ExchangeTimeoutException());
        service.place(USER_ID, limitBuy("100", "30.00"));

        // Exchange reports NOT_FOUND -> order rejected & reservation released
        when(exchange.query(1L)).thenReturn(new ExchangeReport(Status.NOT_FOUND, BigDecimal.ZERO, "Unknown order"));
        Order rejected = service.reconcile(1L);
        assertThat(rejected.getStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(account.getReservedBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void reconcileResolvesOpenOrderExpirationAndCancelRequested() {
        persistOrdersInMemory();
        when(orders.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(exchange.submit(any())).thenReturn(new ExchangeReport(Status.ACCEPTED, BigDecimal.ZERO, null));
        service.place(USER_ID, limitBuy("50", "30.00"));

        when(exchange.query(1L)).thenReturn(new ExchangeReport(Status.EXPIRED, BigDecimal.ZERO, null));
        Order expired = service.reconcile(1L);
        assertThat(expired.getStatus()).isEqualTo(OrderStatus.EXPIRED);
        assertThat(account.getReservedBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void reconcileRetriesCancelWhenOrderIsInCancelRequestedState() {
        persistOrdersInMemory();
        when(orders.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(exchange.submit(any())).thenReturn(new ExchangeReport(Status.ACCEPTED, BigDecimal.ZERO, null));
        service.place(USER_ID, limitBuy("50", "30.00"));
        when(exchange.cancel(1L)).thenThrow(new ExchangeTimeoutException())
                .thenReturn(new ExchangeReport(Status.CANCELLED, BigDecimal.ZERO, null));

        Order afterTimeout = service.cancel(USER_ID, 1L);
        assertThat(afterTimeout.getStatus()).isEqualTo(OrderStatus.CANCEL_REQUESTED);

        Order afterReconcile = service.reconcile(1L);
        assertThat(afterReconcile.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(account.getReservedBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void orderReconcilerProcessesLiveOrdersAndSurvivesIndividualFailures() {
        OrderService spyService = spy(service);
        LimitOrder o1 = new LimitOrder("k1", ACCOUNT_ID, "PETR4", OrderSide.BUY, BigDecimal.TEN, new BigDecimal("30.00"));
        LimitOrder o2 = new LimitOrder("k2", ACCOUNT_ID, "PETR4", OrderSide.BUY, BigDecimal.TEN, new BigDecimal("30.00"));
        ReflectionTestUtils.setField(o1, "id", 101L);
        ReflectionTestUtils.setField(o2, "id", 102L);
        when(orders.findByStatusInAndUpdatedAtBefore(eq(OrderStatus.LIVE), any())).thenReturn(List.of(o1, o2));
        doThrow(new RuntimeException("Simulated DB glitch")).when(spyService).reconcile(101L);
        when(orders.findById(102L)).thenReturn(Optional.of(o2));
        when(exchange.query(102L)).thenThrow(new ExchangeTimeoutException());

        AppProperties props = new AppProperties(null, null, null,
                new AppProperties.Reconciler(Duration.ofSeconds(5), Duration.ofSeconds(2)), null);
        OrderReconciler reconciler = new OrderReconciler(spyService, props);

        reconciler.reconcileLiveOrders();

        verify(spyService).reconcile(101L);
        verify(spyService).reconcile(102L);
    }

    // --- Queries & Ownership -------------------------------------------------

    @Test
    void listAndGetEnforceOwnershipForTradersAndAllowReadAllForAuditors() {
        LimitOrder ownOrder = new LimitOrder("k1", ACCOUNT_ID, "PETR4", OrderSide.BUY, BigDecimal.TEN, new BigDecimal("30.00"));
        LimitOrder otherOrder = new LimitOrder("k2", 999L, "VALE3", OrderSide.BUY, BigDecimal.TEN, new BigDecimal("60.00"));
        ReflectionTestUtils.setField(ownOrder, "id", 10L);
        ReflectionTestUtils.setField(otherOrder, "id", 20L);
        when(orders.findById(10L)).thenReturn(Optional.of(ownOrder));
        when(orders.findById(20L)).thenReturn(Optional.of(otherOrder));
        when(orders.findById(404L)).thenReturn(Optional.empty());

        PageRequest pageReq = PageRequest.of(0, 20);
        when(orders.search(ACCOUNT_ID, OrderStatus.OPEN, "PETR4", pageReq))
                .thenReturn(new PageImpl<>(List.of(ownOrder)));
        when(orders.search(null, null, null, pageReq))
                .thenReturn(new PageImpl<>(List.of(ownOrder, otherOrder)));

        Page<Order> traderList = service.list(USER_ID, false, OrderStatus.OPEN, "PETR4", pageReq);
        assertThat(traderList.getContent()).containsExactly(ownOrder);

        Page<Order> auditorList = service.list(99L, true, null, null, pageReq);
        assertThat(auditorList.getContent()).containsExactly(ownOrder, otherOrder);

        assertThat(service.get(USER_ID, false, 10L)).isSameAs(ownOrder);
        assertThatThrownBy(() -> service.get(USER_ID, false, 20L))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(service.get(99L, true, 20L)).isSameAs(otherOrder);
        assertThatThrownBy(() -> service.get(USER_ID, false, 404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
