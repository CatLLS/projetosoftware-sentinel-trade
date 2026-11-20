package com.sentineltrade.application.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sentineltrade.application.audit.AuditEvent;
import com.sentineltrade.domain.user.MfaVerifier;
import com.sentineltrade.domain.user.Role;
import com.sentineltrade.domain.user.User;
import com.sentineltrade.infrastructure.persistence.UserRepository;
import com.sentineltrade.infrastructure.security.TokenService;
import com.sentineltrade.infrastructure.security.TokenService.Token;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class AuthServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final MfaVerifier mfaVerifier = mock(MfaVerifier.class);
    private final TokenService tokens = mock(TokenService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);

    private AuthService authService;
    private User activeUser;

    @BeforeEach
    void setUp() {
        authService = new AuthService(users, passwordEncoder, mfaVerifier, tokens, events);
        activeUser = new User("trader", passwordEncoder.encode("correct-password"), "JBSWY3DPEHPK3PXP", Role.TRADER);
        ReflectionTestUtils.setField(activeUser, "id", 7L);
    }

    @Test
    void loginWithValidPasswordIssuesPreAuthTokenAndAuditsSuccess() {
        when(users.findByUsername("trader")).thenReturn(Optional.of(activeUser));
        Token expected = new Token("pre-auth-jwt", 300, TokenService.PRE_AUTH);
        when(tokens.preAuth(activeUser)).thenReturn(expected);

        Token result = authService.login("trader", "correct-password");

        assertThat(result).isEqualTo(expected);
        ArgumentCaptor<AuditEvent> event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().type()).isEqualTo("LOGIN_PASSWORD_OK");
        assertThat(event.getValue().actorId()).isEqualTo(7L);
    }

    @Test
    void loginWithWrongPasswordAuditsFailureAndThrowsBadCredentials() {
        when(users.findByUsername("trader")).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> authService.login("trader", "wrong-password"))
                .isInstanceOf(BadCredentialsException.class);

        ArgumentCaptor<AuditEvent> event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().type()).isEqualTo("LOGIN_FAILED");
        assertThat(event.getValue().actorId()).isEqualTo(7L);
    }

    @Test
    void loginWithUnknownOrInactiveUserAuditsFailureAndThrowsBadCredentials() {
        when(users.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("ghost", "any"))
                .isInstanceOf(BadCredentialsException.class);

        ArgumentCaptor<AuditEvent> event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().type()).isEqualTo("LOGIN_FAILED");
        assertThat(event.getValue().actorId()).isNull();
        assertThat(event.getValue().metadata()).containsEntry("username", "ghost");

        User inactive = new User("inactive", passwordEncoder.encode("pass"), "JBSWY3DPEHPK3PXP", Role.TRADER);
        ReflectionTestUtils.setField(inactive, "active", false);
        when(users.findByUsername("inactive")).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> authService.login("inactive", "pass"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void verifyMfaWithValidCodeIssuesFinalAccessTokenAndAuditsSuccess() {
        when(users.findById(7L)).thenReturn(Optional.of(activeUser));
        when(mfaVerifier.verify("JBSWY3DPEHPK3PXP", "123456")).thenReturn(true);
        Token expected = new Token("access-jwt", 900, TokenService.MFA_VERIFIED);
        when(tokens.access(activeUser)).thenReturn(expected);

        Token result = authService.verifyMfa(7L, "123456");

        assertThat(result).isEqualTo(expected);
        ArgumentCaptor<AuditEvent> event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().type()).isEqualTo("MFA_VERIFIED");
        assertThat(event.getValue().actorId()).isEqualTo(7L);
    }

    @Test
    void verifyMfaWithInvalidCodeOrMissingUserThrowsBadCredentials() {
        when(users.findById(7L)).thenReturn(Optional.of(activeUser));
        when(mfaVerifier.verify("JBSWY3DPEHPK3PXP", "000000")).thenReturn(false);

        assertThatThrownBy(() -> authService.verifyMfa(7L, "000000"))
                .isInstanceOf(BadCredentialsException.class);

        ArgumentCaptor<AuditEvent> event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().type()).isEqualTo("MFA_FAILED");

        when(users.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.verifyMfa(999L, "123456"))
                .isInstanceOf(BadCredentialsException.class);
    }
}
