package com.sentineltrade.api.dto;

/** Allow-list format of a B3 ticker, e.g. PETR4 or TAEE11. */
public final class AssetSymbol {

    public static final String PATTERN = "^[A-Z]{4}[0-9]{1,2}$";
    public static final String MESSAGE = "must be a B3 ticker such as PETR4";

    private AssetSymbol() {
    }
}
