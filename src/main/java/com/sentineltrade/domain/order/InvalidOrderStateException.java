package com.sentineltrade.domain.order;

import com.sentineltrade.domain.DomainException;

public class InvalidOrderStateException extends DomainException {

    public InvalidOrderStateException(OrderStatus from, OrderStatus to) {
        this("Order cannot go from " + from + " to " + to);
    }

    public InvalidOrderStateException(String message) {
        super(message);
    }
}
