package com.sentineltrade.application.audit;

import com.sentineltrade.domain.order.Order;
import com.sentineltrade.domain.order.OrderStatus;
import java.util.Map;

/** Published through Spring's ApplicationEventPublisher and persisted by {@link AuditService}. */
public record AuditEvent(String type, Long actorId, String entityName, String entityId,
                         String previousState, String newState, Map<String, Object> metadata) {

    /** A change of state of an order. The actor is resolved from the security context. */
    public static AuditEvent orderTransition(Order order, OrderStatus previous) {
        return new AuditEvent("ORDER_" + order.getStatus(), null, "Order", String.valueOf(order.getId()),
                previous == null ? null : previous.name(), order.getStatus().name(),
                Map.of("type", order.getType(), "side", order.getSide(),
                        "assetSymbol", order.getAssetSymbol(),
                        "executedQuantity", order.getExecutedQuantity()));
    }

    public static AuditEvent order(String type, Order order, Map<String, Object> metadata) {
        return new AuditEvent(type, null, "Order", String.valueOf(order.getId()),
                order.getStatus().name(), order.getStatus().name(), metadata);
    }

    public static AuditEvent security(String type, Long actorId, Map<String, Object> metadata) {
        return new AuditEvent(type, actorId, "User", actorId == null ? null : actorId.toString(),
                null, null, metadata);
    }
}
