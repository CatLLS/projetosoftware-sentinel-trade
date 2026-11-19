package com.sentineltrade.api;

import com.sentineltrade.api.dto.LoginRequest;
import com.sentineltrade.api.dto.MfaRequest;
import com.sentineltrade.api.dto.TokenResponse;
import com.sentineltrade.application.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Two-step login: password, then TOTP code")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @SecurityRequirements // public endpoint
    @Operation(summary = "Step 1: check the password and get a PRE_AUTH token")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return TokenResponse.from(authService.login(request.username(), request.password()));
    }

    @PostMapping("/mfa/verify")
    @Operation(summary = "Step 2: with the PRE_AUTH token, check the TOTP code and get the final token")
    public TokenResponse verifyMfa(Authentication authentication, @Valid @RequestBody MfaRequest request) {
        Long userId = Long.valueOf(authentication.getName());
        return TokenResponse.from(authService.verifyMfa(userId, request.code()));
    }
}
