package com.sentineltrade.domain.risk;

public class RiskLimitExceededException extends RiskViolationException {

    public RiskLimitExceededException() {
        super("Order exceeds the daily risk limit of the account");
    }
}
