package com.sentineltrade.domain.risk;

import com.sentineltrade.domain.DomainException;

/** A pre-trade rule refused the order. */
public abstract class RiskViolationException extends DomainException {

    protected RiskViolationException(String message) {
        super(message);
    }
}
