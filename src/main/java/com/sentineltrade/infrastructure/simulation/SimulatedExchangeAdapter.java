package com.sentineltrade.infrastructure.simulation;

import com.sentineltrade.config.AppProperties;
import com.sentineltrade.domain.exchange.ExchangeGateway;
import com.sentineltrade.domain.exchange.ExchangeReport;
import com.sentineltrade.domain.exchange.ExchangeReport.Status;
import com.sentineltrade.domain.exchange.ExchangeTimeoutException;
import com.sentineltrade.domain.market.MarketQuoteProvider;
import com.sentineltrade.domain.order.Order;
import com.sentineltrade.domain.order.OrderSide;
import com.sentineltrade.domain.order.OrderType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * In-memory exchange. Each time it looks at a live order it executes up to
 * {@code liquidityPerCheck} units when the order is marketable. It can also "lose" the
 * answer to a submission (after accepting it) to exercise reconciliation.
 */
@Component
public class SimulatedExchangeAdapter implements ExchangeGateway {

    private static final ExchangeReport NOT_FOUND =
            new ExchangeReport(Status.NOT_FOUND, BigDecimal.ZERO, "Order unknown to the exchange");

    private final Map<Long, SimOrder> book = new HashMap<>();
    private final MarketQuoteProvider quotes;
    private final AppProperties.Exchange config;

    public SimulatedExchangeAdapter(MarketQuoteProvider quotes, AppProperties properties) {
        this.quotes = quotes;
        this.config = properties.exchange();
    }

    @Override
    public synchronized ExchangeReport submit(Order order) {
        var sim = new SimOrder(order, Instant.now().plus(config.orderTtl()));
        BigDecimal quote = quotes.currentPrice(sim.symbol);
        if (!sim.market && outsidePriceBand(sim.price, quote)) {
            sim.status = Status.REJECTED;
            sim.reason = "Limit price outside the allowed price band";
        } else {
            match(sim, quote);
        }
        book.put(order.getId(), sim);
        if (ThreadLocalRandom.current().nextDouble() < config.timeoutRate()) {
            throw new ExchangeTimeoutException(); // received, but the answer got lost
        }
        return sim.report();
    }

    @Override
    public synchronized ExchangeReport query(Long orderId) {
        SimOrder sim = book.get(orderId);
        if (sim == null) {
            return NOT_FOUND;
        }
        if (sim.isLive()) {
            if (Instant.now().isAfter(sim.expiresAt)) {
                sim.status = Status.EXPIRED;
            } else {
                match(sim, quotes.currentPrice(sim.symbol));
            }
        }
        return sim.report();
    }

    @Override
    public synchronized ExchangeReport cancel(Long orderId) {
        SimOrder sim = book.get(orderId);
        if (sim == null) {
            return NOT_FOUND;
        }
        if (sim.isLive()) {
            sim.status = Status.CANCELLED;
        }
        return sim.report(); // a fully executed order cannot be cancelled anymore
    }

    @Override
    public synchronized ExchangeReport amend(Long orderId, BigDecimal newPrice) {
        SimOrder sim = book.get(orderId);
        if (sim == null) {
            return NOT_FOUND;
        }
        if (sim.isLive() && sim.executed.signum() == 0) {
            sim.price = newPrice;
            match(sim, quotes.currentPrice(sim.symbol));
        }
        return sim.report();
    }

    private void match(SimOrder sim, BigDecimal quote) {
        boolean marketable = sim.market
                || (sim.side == OrderSide.BUY ? quote.compareTo(sim.price) <= 0 : quote.compareTo(sim.price) >= 0);
        if (sim.isLive() && marketable) {
            sim.executed = sim.executed.add(sim.remaining().min(BigDecimal.valueOf(config.liquidityPerCheck())));
        }
    }

    private boolean outsidePriceBand(BigDecimal price, BigDecimal quote) {
        BigDecimal distance = price.subtract(quote).abs().divide(quote, 6, RoundingMode.HALF_UP);
        return distance.compareTo(BigDecimal.valueOf(config.priceBand())) > 0;
    }

    /** The exchange's own copy of an order. */
    private static final class SimOrder {
        final String symbol;
        final OrderSide side;
        final boolean market;
        final BigDecimal quantity;
        final Instant expiresAt;
        BigDecimal price;
        BigDecimal executed = BigDecimal.ZERO;
        Status status = Status.ACCEPTED;
        String reason;

        SimOrder(Order order, Instant expiresAt) {
            this.symbol = order.getAssetSymbol();
            this.side = order.getSide();
            this.market = order.getType() == OrderType.MARKET;
            this.quantity = order.getQuantity();
            this.price = order.getPrice();
            this.expiresAt = expiresAt;
        }

        BigDecimal remaining() {
            return quantity.subtract(executed);
        }

        boolean isLive() {
            return status == Status.ACCEPTED && remaining().signum() > 0;
        }

        ExchangeReport report() {
            return new ExchangeReport(status, executed, reason);
        }
    }
}
