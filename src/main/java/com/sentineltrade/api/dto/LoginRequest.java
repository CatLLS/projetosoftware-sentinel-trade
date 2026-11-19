package com.sentineltrade.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 64) @Schema(example = "trader") String username,
        @NotBlank @Size(max = 128) String password) {
}
