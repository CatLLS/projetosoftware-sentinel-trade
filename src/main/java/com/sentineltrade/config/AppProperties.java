package com.sentineltrade.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Typed view of the {@code app.*} section of application.yml. */
@ConfigurationProperties("app")
public record AppProperties(Security security, Market market, Exchange exchange,
                            Reconciler reconciler, Seed seed) {

    public record Security(String jwtSecret, String jwtIssuer, Duration preAuthTokenTtl,
                           Duration accessTokenTtl, String mfaEncryptionKey) {
    }

    public record Market(double volatility) {
    }

    /**
     * @param timeoutRate       share of submissions whose answer is "lost" (0..1)
     * @param liquidityPerCheck max quantity executed each time the exchange looks at an order
     * @param priceBand         max relative distance between a limit price and the market
     */
    public record Exchange(double timeoutRate, long liquidityPerCheck, double priceBand,
                           Duration orderTtl) {
    }

    public record Reconciler(Duration interval, Duration grace) {
    }

    public record Seed(boolean enabled, String password, String mfaSecret) {
    }
}
