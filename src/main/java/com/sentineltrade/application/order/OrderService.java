package com.sentineltrade.application.order;

import com.sentineltrade.application.ResourceNotFoundException;
import com.sentineltrade.application.audit.AuditEvent;
import com.sentineltrade.domain.account.Account;
import com.sentineltrade.domain.account.Position;
import com.sentineltrade.domain.account.Settlement;
import com.sentineltrade.domain.exchange.ExchangeGateway;
import com.sentineltrade.domain.exchange.ExchangeReport;
import com.sentineltrade.domain.exchange.ExchangeTimeoutException;
import com.sentineltrade.domain.market.MarketQuoteProvider;
import com.sentineltrade.domain.order.DuplicateOrderException;
import com.sentineltrade.domain.order.InvalidOrderStateException;
import com.sentineltrade.domain.order.LimitOrder;
import com.sentineltrade.domain.order.Order;
import com.sentineltrade.domain.order.OrderStatus;
import com.sentineltrade.domain.risk.RiskContext;
import com.sentineltrade.domain.risk.RiskRule;
import com.sentineltrade.domain.risk.RiskViolationException;
import com.sentineltrade.infrastructure.persistence.AccountRepository;
import com.sentineltrade.infrastructure.persistence.OrderRepository;
import com.sentineltrade.infrastructure.persistence.PositionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Orchestrates the order use cases. Each state change runs in its own short ACID transaction
 * together with its audit record; calls to the exchange happen between transactions, never
 * inside one.
 */
@Service
public class OrderService {

    private static final ZoneId MARKET_ZONE = ZoneId.of("America/Sao_Paulo");

    public record Placement(Order order, boolean created) {
    }

    private final OrderRepository orders;
    private final AccountRepository accounts;
    private final PositionRepository positions;
    private final List<RiskRule> riskRules;
    private final MarketQuoteProvider quotes;
    private final ExchangeGateway exchange;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate tx;

    public OrderService(OrderRepository orders, AccountRepository accounts, PositionRepository positions,
                        List<RiskRule> riskRules, MarketQuoteProvider quotes, ExchangeGateway exchange,
                        ApplicationEventPublisher events, TransactionTemplate tx) {
        this.orders = orders;
        this.accounts = accounts;
        this.positions = positions;
        this.riskRules = riskRules;
        this.quotes = quotes;
        this.exchange = exchange;
        this.events = events;
        this.tx = tx;
    }

    // --- Commands ------------------------------------------------------------

    /** Creates, validates, reserves and submits an order. Replays return the original order. */
    public Placement place(Long userId, PlaceOrderCommand cmd) {
        Account account = accountOf(userId);
        var existing = orders.findByIdempotencyKey(cmd.idempotencyKey());
        if (existing.isPresent()) {
            return new Placement(replay(account, cmd, existing.get()), false);
        }

        Order order = cmd.toOrder(account.getId(), quotes.currentPrice(cmd.assetSymbol()));
        RiskViolationException violation = tx.execute(status -> validateAndReserve(order));
        if (violation != null) {
            throw violation; // the REJECTED order and its audit record are already committed
        }
        ExchangeReport report = callExchange(() -> exchange.submit(order));
        Order sent = report == null
                ? update(order.getId(), o -> change(o, o::markPendingReconciliation)) // never resubmit
                : update(order.getId(), o -> apply(o, report));
        return new Placement(sent, true);
    }

    public Order cancel(Long userId, Long orderId) {
        Long accountId = accountOf(userId).getId();
        Order order = update(orderId, o -> {
            requireOwner(o, accountId);
            change(o, o::requestCancel);
        });
        return applyFromExchange(order.getId(), () -> exchange.cancel(orderId));
    }

    /** Changes the price of an open limit order, adjusting the reservation accordingly. */
    public Order amend(Long userId, Long orderId, BigDecimal newPrice) {
        Long accountId = accountOf(userId).getId();
        update(orderId, o -> {
            requireOwner(o, accountId);
            if (!(o instanceof LimitOrder limit)) {
                throw new InvalidOrderStateException("Only limit orders can be amended");
            }
            Account account = accountFor(o);
            Position position = positionFor(o);
            BigDecimal oldPrice = limit.getPrice();
            Settlement.releaseRemaining(limit, account, position);
            limit.changeLimitPrice(newPrice);
            Settlement.reserve(limit, account, position);
            events.publishEvent(AuditEvent.order("ORDER_AMENDED", limit,
                    Map.of("oldPrice", oldPrice, "newPrice", newPrice)));
        });
        return applyFromExchange(orderId, () -> exchange.amend(orderId, newPrice));
    }

    /** Asks the exchange for the real state of a live order. Never resubmits. */
    public Order reconcile(Long orderId) {
        Order order = find(orderId);
        return order.getStatus() == OrderStatus.CANCEL_REQUESTED
                ? applyFromExchange(orderId, () -> exchange.cancel(orderId)) // cancelling is idempotent
                : applyFromExchange(orderId, () -> exchange.query(orderId));
    }

    // --- Queries -------------------------------------------------------------

    public Page<Order> list(Long userId, boolean readAll, OrderStatus status, String assetSymbol,
                            Pageable pageable) {
        Long accountId = readAll ? null : accountOf(userId).getId();
        return orders.search(accountId, status, assetSymbol, pageable);
    }

    public Order get(Long userId, boolean readAll, Long orderId) {
        Order order = find(orderId);
        if (!readAll) {
            requireOwner(order, accountOf(userId).getId());
        }
        return order;
    }

    public List<Long> liveOrderIds(Instant updatedBefore) {
        return orders.findByStatusInAndUpdatedAtBefore(OrderStatus.LIVE, updatedBefore).stream()
                .map(Order::getId)
                .toList();
    }

    // --- Internals -----------------------------------------------------------

    private Order replay(Account account, PlaceOrderCommand cmd, Order existing) {
        boolean sameRequest = existing.getAccountId().equals(account.getId()) && cmd.matches(existing);
        events.publishEvent(AuditEvent.order(
                sameRequest ? "ORDER_DUPLICATE_REPLAYED" : "ORDER_DUPLICATE_BLOCKED", existing,
                Map.of("idempotencyKey", cmd.idempotencyKey())));
        if (!sameRequest) {
            throw new DuplicateOrderException();
        }
        return existing;
    }

    /** Runs inside a transaction. Returns the violation instead of throwing, so the rejection commits. */
    private RiskViolationException validateAndReserve(Order order) {
        Account account = accountFor(order);
        Position position = positions
                .findByAccountIdAndAssetSymbol(order.getAccountId(), order.getAssetSymbol())
                .orElse(null);
        BigDecimal tradedToday = Objects.requireNonNullElse(
                orders.notionalPlacedSince(account.getId(), startOfToday()), BigDecimal.ZERO);
        var context = new RiskContext(account, position, tradedToday);

        orders.save(order);
        events.publishEvent(AuditEvent.orderTransition(order, null));
        try {
            riskRules.forEach(rule -> rule.check(order, context));
        } catch (RiskViolationException e) {
            change(order, () -> order.reject(e.getMessage()));
            return e;
        }
        change(order, order::validate);
        Settlement.reserve(order, account, position);
        change(order, order::markSent);
        return null;
    }

    /** Translates what the exchange reports into domain transitions and settlement. */
    private void apply(Order order, ExchangeReport report) {
        Account account = accountFor(order);
        Position position = positionFor(order);
        switch (report.status()) {
            case REJECTED, NOT_FOUND -> {
                Settlement.releaseRemaining(order, account, position);
                change(order, () -> order.reject(report.reason()));
            }
            case ACCEPTED, CANCELLED, EXPIRED -> {
                if (order.getStatus() == OrderStatus.SENT
                        || order.getStatus() == OrderStatus.PENDING_RECONCILIATION) {
                    change(order, order::accept);
                }
                BigDecimal filled = report.executedQuantity().subtract(order.getExecutedQuantity());
                if (filled.signum() > 0) {
                    Settlement.settleFill(order, filled, account, position);
                    positions.save(position);
                    change(order, () -> order.registerExecution(filled));
                }
                if (order.getStatus().isFinal() || report.status() == ExchangeReport.Status.ACCEPTED) {
                    return;
                }
                Settlement.releaseRemaining(order, account, position);
                change(order, report.status() == ExchangeReport.Status.CANCELLED
                        ? order::confirmCancel
                        : order::expire);
            }
        }
    }

    /** Calls the exchange and applies its report; on timeout the order stays as it is. */
    private Order applyFromExchange(Long orderId, Supplier<ExchangeReport> call) {
        ExchangeReport report = callExchange(call);
        return report == null ? find(orderId) : update(orderId, o -> apply(o, report));
    }

    /** @return the report, or {@code null} when the exchange timed out */
    private static ExchangeReport callExchange(Supplier<ExchangeReport> call) {
        try {
            return call.get();
        } catch (ExchangeTimeoutException e) {
            return null;
        }
    }

    /** Applies a state change and publishes its audit event (same transaction). */
    private void change(Order order, Runnable transition) {
        OrderStatus previous = order.getStatus();
        transition.run();
        events.publishEvent(AuditEvent.orderTransition(order, previous));
    }

    /** Loads the order, runs the action and commits, all in one transaction. */
    private Order update(Long orderId, Consumer<Order> action) {
        return tx.execute(status -> {
            Order order = find(orderId);
            action.accept(order);
            return order;
        });
    }

    private Order find(Long orderId) {
        return orders.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order"));
    }

    private Account accountOf(Long userId) {
        return accounts.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException("Account"));
    }

    private Account accountFor(Order order) {
        return accounts.findById(order.getAccountId()).orElseThrow();
    }

    /** Existing position, or a new empty one (saved only when a purchase fills). */
    private Position positionFor(Order order) {
        return positions.findByAccountIdAndAssetSymbol(order.getAccountId(), order.getAssetSymbol())
                .orElseGet(() -> new Position(order.getAccountId(), order.getAssetSymbol()));
    }

    /** Someone else's order is reported as missing, so its existence does not leak. */
    private static void requireOwner(Order order, Long accountId) {
        if (!order.getAccountId().equals(accountId)) {
            throw new ResourceNotFoundException("Order");
        }
    }

    private static Instant startOfToday() {
        return LocalDate.now(MARKET_ZONE).atStartOfDay(MARKET_ZONE).toInstant();
    }
}
