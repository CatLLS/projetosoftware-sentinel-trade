package com.sentineltrade.domain.market;

import java.math.BigDecimal;

/** Port for market data; implemented by an adapter in the infrastructure layer. */
public interface MarketQuoteProvider {

    /** @throws UnknownAssetException if the symbol is not traded */
    BigDecimal currentPrice(String assetSymbol);
}
