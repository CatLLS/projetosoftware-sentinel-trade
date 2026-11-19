package com.sentineltrade.domain.risk;

public class InsufficientBalanceException extends RiskViolationException {

    public InsufficientBalanceException() {
        super("Insufficient available balance");
    }
}
