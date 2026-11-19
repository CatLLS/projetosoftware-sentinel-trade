package com.sentineltrade.infrastructure.simulation;

import com.sentineltrade.config.AppProperties;
import com.sentineltrade.domain.market.MarketQuoteProvider;
import com.sentineltrade.domain.market.UnknownAssetException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/** In-memory quotes for a few B3 assets, moving by a small random step on every read. */
@Component
public class SimulatedMarketQuoteAdapter implements MarketQuoteProvider {

    private final Map<String, BigDecimal> prices = new ConcurrentHashMap<>(Map.of(
            "PETR4", new BigDecimal("37.50"),
            "VALE3", new BigDecimal("61.20"),
            "ITUB4", new BigDecimal("33.80"),
            "BBDC4", new BigDecimal("14.90"),
            "ABEV3", new BigDecimal("12.70"),
            "WEGE3", new BigDecimal("41.60")));

    private final double volatility;

    public SimulatedMarketQuoteAdapter(AppProperties properties) {
        this.volatility = properties.market().volatility();
    }

    @Override
    public BigDecimal currentPrice(String assetSymbol) {
        BigDecimal price = prices.computeIfPresent(assetSymbol, (symbol, current) -> randomStep(current));
        if (price == null) {
            throw new UnknownAssetException();
        }
        return price;
    }

    private BigDecimal randomStep(BigDecimal price) {
        if (volatility <= 0) {
            return price;
        }
        double factor = 1 + ThreadLocalRandom.current().nextDouble(-volatility, volatility);
        return price.multiply(BigDecimal.valueOf(factor)).setScale(2, RoundingMode.HALF_UP);
    }
}
