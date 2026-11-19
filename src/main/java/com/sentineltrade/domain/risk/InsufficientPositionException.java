package com.sentineltrade.domain.risk;

public class InsufficientPositionException extends RiskViolationException {

    public InsufficientPositionException() {
        super("Insufficient free position in the asset");
    }
}
