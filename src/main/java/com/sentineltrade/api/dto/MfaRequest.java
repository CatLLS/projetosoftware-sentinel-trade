package com.sentineltrade.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record MfaRequest(
        @NotNull @Pattern(regexp = "^\\d{6}$", message = "must be a 6-digit code")
        @Schema(example = "123456") String code) {
}
