package com.sentineltrade.domain.exchange;

/** The exchange did not answer in time; the outcome of the call is unknown. */
public class ExchangeTimeoutException extends RuntimeException {

    public ExchangeTimeoutException() {
        super("Exchange did not answer in time");
    }
}
