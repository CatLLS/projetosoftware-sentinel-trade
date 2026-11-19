package com.sentineltrade.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sentineltrade.application.order.PlaceOrderCommand;
import com.sentineltrade.domain.order.OrderSide;
import com.sentineltrade.domain.order.OrderType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record PlaceOrderRequest(
        @NotNull OrderType type,
        @NotNull @Pattern(regexp = AssetSymbol.PATTERN, message = AssetSymbol.MESSAGE)
        @Schema(example = "PETR4") String assetSymbol,
        @NotNull OrderSide side,
        @NotNull @Positive @Digits(integer = 9, fraction = 0, message = "must be a whole number of shares")
        @Schema(example = "100") BigDecimal quantity,
        @Positive @Digits(integer = 13, fraction = 4)
        @Schema(description = "Required for LIMIT orders, forbidden for MARKET orders", example = "36.50")
        BigDecimal limitPrice) {

    @JsonIgnore
    @AssertTrue(message = "limitPrice is required for LIMIT orders and forbidden for MARKET orders")
    public boolean isLimitPriceConsistent() {
        return type == null || (type == OrderType.LIMIT) == (limitPrice != null);
    }

    public PlaceOrderCommand toCommand(String idempotencyKey) {
        return new PlaceOrderCommand(idempotencyKey, type, assetSymbol, side, quantity, limitPrice);
    }
}
