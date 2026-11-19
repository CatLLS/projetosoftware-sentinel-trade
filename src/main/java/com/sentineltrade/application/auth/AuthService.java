package com.sentineltrade.application.auth;

import com.sentineltrade.application.audit.AuditEvent;
import com.sentineltrade.domain.user.MfaVerifier;
import com.sentineltrade.domain.user.User;
import com.sentineltrade.infrastructure.persistence.UserRepository;
import com.sentineltrade.infrastructure.security.TokenService;
import com.sentineltrade.infrastructure.security.TokenService.Token;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Two-step login: password first (PRE_AUTH token), then the TOTP code (final token). */
@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final MfaVerifier mfaVerifier;
    private final TokenService tokens;
    private final ApplicationEventPublisher events;
    /** Compared against when the user does not exist, so timing does not reveal valid usernames. */
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, MfaVerifier mfaVerifier,
                       TokenService tokens, ApplicationEventPublisher events) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.mfaVerifier = mfaVerifier;
        this.tokens = tokens;
        this.events = events;
        this.dummyHash = passwordEncoder.encode("dummy-password");
    }

    public Token login(String username, String password) {
        User user = users.findByUsername(username).filter(User::isActive).orElse(null);
        boolean valid = passwordEncoder.matches(password, user != null ? user.getPasswordHash() : dummyHash);
        if (user == null || !valid) {
            events.publishEvent(AuditEvent.security("LOGIN_FAILED", user == null ? null : user.getId(),
                    Map.of("username", username)));
            throw new BadCredentialsException("Invalid credentials");
        }
        events.publishEvent(AuditEvent.security("LOGIN_PASSWORD_OK", user.getId(), Map.of()));
        return tokens.preAuth(user);
    }

    public Token verifyMfa(Long userId, String code) {
        User user = users.findById(userId).filter(User::isActive)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!mfaVerifier.verify(user.getMfaSecret(), code)) {
            events.publishEvent(AuditEvent.security("MFA_FAILED", user.getId(), Map.of()));
            throw new BadCredentialsException("Invalid MFA code");
        }
        events.publishEvent(AuditEvent.security("MFA_VERIFIED", user.getId(), Map.of()));
        return tokens.access(user);
    }
}
