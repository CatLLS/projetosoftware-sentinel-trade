package com.sentineltrade.api.dto;

import com.sentineltrade.domain.order.Order;
import com.sentineltrade.domain.order.OrderSide;
import com.sentineltrade.domain.order.OrderStatus;
import com.sentineltrade.domain.order.OrderType;
import java.math.BigDecimal;
import java.time.Instant;

/** <<no down-flow>>: the API exposes this DTO, never the JPA entity. */
public record OrderResponse(Long id, Long accountId, OrderType type, String assetSymbol, OrderSide side,
                            BigDecimal quantity, BigDecimal executedQuantity, BigDecimal price,
                            OrderStatus status, String rejectionReason, Instant createdAt,
                            Instant updatedAt) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getAccountId(), order.getType(),
                order.getAssetSymbol(), order.getSide(), order.getQuantity(), order.getExecutedQuantity(),
                order.getPrice(), order.getStatus(), order.getRejectionReason(), order.getCreatedAt(),
                order.getUpdatedAt());
    }
}
