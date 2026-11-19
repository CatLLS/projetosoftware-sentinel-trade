package com.sentineltrade.domain.order;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Order lifecycle, as specified in Docs/uml/uml-estados.md. */
public enum OrderStatus {
    CREATED,
    VALIDATED,
    SENT,
    OPEN,
    PENDING_RECONCILIATION,
    PARTIALLY_EXECUTED,
    CANCEL_REQUESTED,
    EXECUTED,
    REJECTED,
    CANCELLED,
    EXPIRED;

    private static final Map<OrderStatus, Set<OrderStatus>> NEXT = Map.of(
            CREATED, EnumSet.of(VALIDATED, REJECTED),
            VALIDATED, EnumSet.of(SENT),
            SENT, EnumSet.of(OPEN, REJECTED, PENDING_RECONCILIATION),
            PENDING_RECONCILIATION, EnumSet.of(OPEN, PARTIALLY_EXECUTED, EXECUTED, REJECTED),
            OPEN, EnumSet.of(PARTIALLY_EXECUTED, EXECUTED, CANCEL_REQUESTED, EXPIRED),
            PARTIALLY_EXECUTED, EnumSet.of(PARTIALLY_EXECUTED, EXECUTED, CANCEL_REQUESTED, EXPIRED),
            CANCEL_REQUESTED, EnumSet.of(CANCELLED, EXECUTED));

    /** Orders in these states are still alive at the exchange and must be tracked. */
    public static final Set<OrderStatus> LIVE =
            EnumSet.of(SENT, PENDING_RECONCILIATION, OPEN, PARTIALLY_EXECUTED, CANCEL_REQUESTED);

    public boolean canTransitionTo(OrderStatus target) {
        return NEXT.getOrDefault(this, Set.of()).contains(target);
    }

    public boolean isFinal() {
        return !NEXT.containsKey(this);
    }
}
