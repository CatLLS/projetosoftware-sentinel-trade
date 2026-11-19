package com.sentineltrade.domain.order;

import com.sentineltrade.domain.DomainException;

public class DuplicateOrderException extends DomainException {

    public DuplicateOrderException() {
        super("Idempotency key was already used for a different order");
    }
}
