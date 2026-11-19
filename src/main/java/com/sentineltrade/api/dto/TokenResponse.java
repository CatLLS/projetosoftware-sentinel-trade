package com.sentineltrade.api.dto;

import com.sentineltrade.infrastructure.security.TokenService.Token;

/** @param stage PRE_AUTH (call /auth/mfa/verify next) or MFA_VERIFIED */
public record TokenResponse(String accessToken, String tokenType, long expiresIn, String stage) {

    public static TokenResponse from(Token token) {
        return new TokenResponse(token.value(), "Bearer", token.expiresInSeconds(), token.stage());
    }
}
