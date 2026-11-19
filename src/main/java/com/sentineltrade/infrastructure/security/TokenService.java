package com.sentineltrade.infrastructure.security;

import com.sentineltrade.config.AppProperties;
import com.sentineltrade.domain.user.User;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * Issues the two tokens of the login flow. The PRE_AUTH token only opens the MFA endpoint;
 * the MFA_VERIFIED token carries the user's role and opens the rest of the API.
 */
@Component
public class TokenService {

    public static final String PRE_AUTH = "PRE_AUTH";
    public static final String MFA_VERIFIED = "MFA_VERIFIED";
    public static final String AUTHORITIES_CLAIM = "authorities";

    public record Token(String value, long expiresInSeconds, String stage) {
    }

    private final JwtEncoder encoder;
    private final AppProperties.Security config;

    public TokenService(JwtEncoder encoder, AppProperties properties) {
        this.encoder = encoder;
        this.config = properties.security();
    }

    public Token preAuth(User user) {
        return issue(user, List.of(PRE_AUTH), config.preAuthTokenTtl(), PRE_AUTH);
    }

    public Token access(User user) {
        return issue(user, List.of(user.getRole().authority(), MFA_VERIFIED),
                config.accessTokenTtl(), MFA_VERIFIED);
    }

    private Token issue(User user, List<String> authorities, Duration ttl, String stage) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(config.jwtIssuer())
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .claim(AUTHORITIES_CLAIM, authorities)
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new Token(value, ttl.toSeconds(), stage);
    }
}
