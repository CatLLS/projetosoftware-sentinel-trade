package com.sentineltrade.domain;

/** Base class for business-rule violations. Messages are safe to show to clients. */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
