package com.sentineltrade.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record AmendOrderRequest(
        @NotNull @Positive @Digits(integer = 13, fraction = 4) @Schema(example = "36.80") BigDecimal limitPrice) {
}
