package com.sentineltrade.infrastructure.simulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sentineltrade.config.AppProperties;
import com.sentineltrade.domain.exchange.ExchangeReport;
import com.sentineltrade.domain.exchange.ExchangeReport.Status;
import com.sentineltrade.domain.exchange.ExchangeTimeoutException;
import com.sentineltrade.domain.market.UnknownAssetException;
import com.sentineltrade.domain.order.LimitOrder;
import com.sentineltrade.domain.order.MarketOrder;
import com.sentineltrade.domain.order.OrderSide;
import java.math.BigDecimal;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SimulationAdaptersTest {

    private static AppProperties props(double volatility, double timeoutRate, long liquidityPerCheck,
                                       double priceBand, Duration orderTtl) {
        return new AppProperties(
                null,
                new AppProperties.Market(volatility),
                new AppProperties.Exchange(timeoutRate, liquidityPerCheck, priceBand, orderTtl),
                null,
                null);
    }

    // --- SimulatedMarketQuoteAdapter -----------------------------------------

    @Test
    void marketQuotesReturnConstantPriceWhenVolatilityIsZeroAndApplyDriftWhenPositive() {
        SimulatedMarketQuoteAdapter staticQuotes = new SimulatedMarketQuoteAdapter(
                props(0.0, 0.0, 1000, 0.20, Duration.ofHours(8)));

        assertThat(staticQuotes.currentPrice("PETR4")).isEqualByComparingTo("37.50");
        assertThat(staticQuotes.currentPrice("PETR4")).isEqualByComparingTo("37.50");
        assertThat(staticQuotes.currentPrice("VALE3")).isEqualByComparingTo("61.20");
        assertThat(staticQuotes.currentPrice("ITUB4")).isEqualByComparingTo("33.80");
        assertThat(staticQuotes.currentPrice("BBDC4")).isEqualByComparingTo("14.90");
        assertThat(staticQuotes.currentPrice("ABEV3")).isEqualByComparingTo("12.70");
        assertThat(staticQuotes.currentPrice("WEGE3")).isEqualByComparingTo("41.60");

        assertThatThrownBy(() -> staticQuotes.currentPrice("INVALID9"))
                .isInstanceOf(UnknownAssetException.class);

        SimulatedMarketQuoteAdapter volatileQuotes = new SimulatedMarketQuoteAdapter(
                props(0.05, 0.0, 1000, 0.20, Duration.ofHours(8)));
        BigDecimal moved = volatileQuotes.currentPrice("PETR4");
        assertThat(moved).isBetween(new BigDecimal("35.00"), new BigDecimal("40.00"));
    }

    // --- SimulatedExchangeAdapter --------------------------------------------

    @Test
    void exchangeExecutesMarketAndMarketableLimitOrdersAndRestsNonMarketableOnes() {
        AppProperties p = props(0.0, 0.0, 50, 0.20, Duration.ofHours(8));
        SimulatedMarketQuoteAdapter quotes = new SimulatedMarketQuoteAdapter(p);
        SimulatedExchangeAdapter exchange = new SimulatedExchangeAdapter(quotes, p);

        // Market order with quantity 80 and liquidityPerCheck 50 -> partial fill of 50 on submit, 30 on query
        MarketOrder marketBuy = new MarketOrder("k1", 1L, "PETR4", OrderSide.BUY,
                new BigDecimal("80"), new BigDecimal("37.50"));
        ReflectionTestUtils.setField(marketBuy, "id", 1L);

        ExchangeReport r1 = exchange.submit(marketBuy);
        assertThat(r1.status()).isEqualTo(Status.ACCEPTED);
        assertThat(r1.executedQuantity()).isEqualByComparingTo("50");

        ExchangeReport r2 = exchange.query(1L);
        assertThat(r2.status()).isEqualTo(Status.ACCEPTED);
        assertThat(r2.executedQuantity()).isEqualByComparingTo("80");

        // Non-marketable limit buy (35.00 < 37.50) rests on book, then amended to 38.00 and executes
        LimitOrder limitBuy = new LimitOrder("k2", 1L, "PETR4", OrderSide.BUY,
                new BigDecimal("20"), new BigDecimal("35.00"));
        ReflectionTestUtils.setField(limitBuy, "id", 2L);

        ExchangeReport resting = exchange.submit(limitBuy);
        assertThat(resting.status()).isEqualTo(Status.ACCEPTED);
        assertThat(resting.executedQuantity()).isEqualByComparingTo("0");

        ExchangeReport afterAmend = exchange.amend(2L, new BigDecimal("38.00"));
        assertThat(afterAmend.executedQuantity()).isEqualByComparingTo("20");

        // Limit order outside 20% price band is rejected
        LimitOrder outsideBand = new LimitOrder("k3", 1L, "PETR4", OrderSide.BUY,
                new BigDecimal("10"), new BigDecimal("20.00"));
        ReflectionTestUtils.setField(outsideBand, "id", 3L);

        ExchangeReport rejected = exchange.submit(outsideBand);
        assertThat(rejected.status()).isEqualTo(Status.REJECTED);
        assertThat(rejected.reason()).contains("price band");
    }

    @Test
    void exchangeHandlesCancelExpirationTimeoutAndUnknownOrders() {
        AppProperties p = props(0.0, 0.0, 1000, 0.20, Duration.ofHours(8));
        SimulatedMarketQuoteAdapter quotes = new SimulatedMarketQuoteAdapter(p);
        SimulatedExchangeAdapter exchange = new SimulatedExchangeAdapter(quotes, p);

        assertThat(exchange.query(999L).status()).isEqualTo(Status.NOT_FOUND);
        assertThat(exchange.cancel(999L).status()).isEqualTo(Status.NOT_FOUND);
        assertThat(exchange.amend(999L, BigDecimal.TEN).status()).isEqualTo(Status.NOT_FOUND);

        // Resting SELL order (40.00 > 37.50) gets cancelled
        LimitOrder restingSell = new LimitOrder("k4", 1L, "PETR4", OrderSide.SELL,
                new BigDecimal("10"), new BigDecimal("40.00"));
        ReflectionTestUtils.setField(restingSell, "id", 4L);
        exchange.submit(restingSell);

        ExchangeReport cancelled = exchange.cancel(4L);
        assertThat(cancelled.status()).isEqualTo(Status.CANCELLED);

        // Zero/negative TTL expires order on next query
        AppProperties shortTtlProps = props(0.0, 0.0, 1000, 0.20, Duration.ofSeconds(-1));
        SimulatedExchangeAdapter expiringExchange = new SimulatedExchangeAdapter(quotes, shortTtlProps);
        LimitOrder expiringOrder = new LimitOrder("k5", 1L, "PETR4", OrderSide.BUY,
                new BigDecimal("10"), new BigDecimal("35.00"));
        ReflectionTestUtils.setField(expiringOrder, "id", 5L);
        expiringExchange.submit(expiringOrder);
        assertThat(expiringExchange.query(5L).status()).isEqualTo(Status.EXPIRED);

        // Timeout rate = 1.0 records the order in the book and throws ExchangeTimeoutException
        AppProperties timeoutProps = props(0.0, 1.0, 1000, 0.20, Duration.ofHours(8));
        SimulatedExchangeAdapter timeoutExchange = new SimulatedExchangeAdapter(quotes, timeoutProps);
        MarketOrder timedOutOrder = new MarketOrder("k6", 1L, "PETR4", OrderSide.BUY,
                new BigDecimal("15"), new BigDecimal("37.50"));
        ReflectionTestUtils.setField(timedOutOrder, "id", 6L);

        assertThatThrownBy(() -> timeoutExchange.submit(timedOutOrder))
                .isInstanceOf(ExchangeTimeoutException.class);
        // Querying by id resolves the true state (already executed on the exchange!)
        ExchangeReport reconciled = timeoutExchange.query(6L);
        assertThat(reconciled.status()).isEqualTo(Status.ACCEPTED);
        assertThat(reconciled.executedQuantity()).isEqualByComparingTo("15");
    }
}
