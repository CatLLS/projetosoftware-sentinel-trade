package com.sentineltrade.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentineltrade.application.audit.AuditEvent;
import com.sentineltrade.config.AppProperties;
import com.sentineltrade.domain.user.Role;
import com.sentineltrade.domain.user.User;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class SecurityInfrastructureTest {

    private static AppProperties appProperties(String jwtSecret, String mfaKey) {
        return new AppProperties(
                new AppProperties.Security(jwtSecret, "sentinel-trade", Duration.ofMinutes(5),
                        Duration.ofMinutes(15), mfaKey),
                null, null, null, null);
    }

    // --- SecretConverter -----------------------------------------------------

    @Test
    void secretConverterEncryptsAndDecryptsWithRandomIv() {
        SecretConverter converter = new SecretConverter(
                appProperties("01234567890123456789012345678901", "test-aes-passphrase"));

        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();

        String plain = "JBSWY3DPEHPK3PXP";
        String c1 = converter.convertToDatabaseColumn(plain);
        String c2 = converter.convertToDatabaseColumn(plain);

        assertThat(c1).isNotEqualTo(plain);
        assertThat(c1).isNotEqualTo(c2); // distinct random 12-byte IVs
        assertThat(converter.convertToEntityAttribute(c1)).isEqualTo(plain);
        assertThat(converter.convertToEntityAttribute(c2)).isEqualTo(plain);
    }

    @Test
    void secretConverterRejectsBlankKeyOrTamperedCiphertext() {
        assertThatThrownBy(() -> new SecretConverter(appProperties("01234567890123456789012345678901", "   ")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MFA_SECRET_ENCRYPTION_KEY");

        SecretConverter converter = new SecretConverter(
                appProperties("01234567890123456789012345678901", "test-aes-passphrase"));
        byte[] bytes = Base64.getDecoder().decode(converter.convertToDatabaseColumn("JBSWY3DPEHPK3PXP"));
        bytes[bytes.length - 1] ^= 0x01; // flip last bit of GCM auth tag
        String tampered = Base64.getEncoder().encodeToString(bytes);

        assertThatThrownBy(() -> converter.convertToEntityAttribute(tampered))
                .isInstanceOf(IllegalStateException.class);
    }

    // --- SecurityConfig & TokenService ---------------------------------------

    @Test
    void securityConfigRejectsShortJwtKeyAndEncodesValidTokens() {
        SecurityConfig config = new SecurityConfig();
        assertThatThrownBy(() -> config.jwtKey(appProperties("too-short", "mfa-key")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");

        AppProperties props = appProperties("0123456789abcdef0123456789abcdef", "mfa-key");
        SecretKey key = config.jwtKey(props);
        JwtEncoder encoder = config.jwtEncoder(key);
        JwtDecoder decoder = config.jwtDecoder(key, props);
        TokenService tokenService = new TokenService(encoder, props);

        User trader = new User("trader", "hash", "JBSWY3DPEHPK3PXP", Role.TRADER);
        ReflectionTestUtils.setField(trader, "id", 15L);

        TokenService.Token preAuth = tokenService.preAuth(trader);
        assertThat(preAuth.stage()).isEqualTo(TokenService.PRE_AUTH);
        assertThat(preAuth.expiresInSeconds()).isEqualTo(300L);
        Jwt preAuthJwt = decoder.decode(preAuth.value());
        assertThat(preAuthJwt.getSubject()).isEqualTo("15");
        assertThat(preAuthJwt.getClaimAsStringList(TokenService.AUTHORITIES_CLAIM))
                .containsExactly(TokenService.PRE_AUTH);

        TokenService.Token access = tokenService.access(trader);
        assertThat(access.stage()).isEqualTo(TokenService.MFA_VERIFIED);
        assertThat(access.expiresInSeconds()).isEqualTo(900L);
        Jwt accessJwt = decoder.decode(access.value());
        assertThat(accessJwt.getClaimAsStringList(TokenService.AUTHORITIES_CLAIM))
                .containsExactly("ROLE_TRADER", TokenService.MFA_VERIFIED);

        assertThat(config.passwordEncoder().matches("secret", config.passwordEncoder().encode("secret"))).isTrue();
    }

    // --- SecurityAuditHandler ------------------------------------------------

    @Test
    void securityAuditHandlerWritesRfc7807ProblemDetailsAndAuditsAccessDenied() throws Exception {
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        SecurityAuditHandler handler = new SecurityAuditHandler(events, new ObjectMapper());

        MockHttpServletRequest req401 = new MockHttpServletRequest("GET", "/api/v1/orders");
        MockHttpServletResponse res401 = new MockHttpServletResponse();
        handler.commence(req401, res401, new BadCredentialsException("missing token"));
        assertThat(res401.getStatus()).isEqualTo(401);
        assertThat(res401.getContentType()).isEqualTo("application/problem+json");
        assertThat(res401.getContentAsString()).contains("Authentication required");

        MockHttpServletRequest req403 = new MockHttpServletRequest("POST", "/api/v1/orders");
        MockHttpServletResponse res403 = new MockHttpServletResponse();
        handler.handle(req403, res403, new AccessDeniedException("missing MFA_VERIFIED"));
        assertThat(res403.getStatus()).isEqualTo(403);
        assertThat(res403.getContentType()).isEqualTo("application/problem+json");
        assertThat(res403.getContentAsString()).contains("Access denied");

        ArgumentCaptor<AuditEvent> audit = ArgumentCaptor.forClass(AuditEvent.class);
        verify(events).publishEvent(audit.capture());
        assertThat(audit.getValue().type()).isEqualTo("ACCESS_DENIED");
        assertThat(audit.getValue().metadata()).containsEntry("method", "POST")
                .containsEntry("path", "/api/v1/orders");
    }
}
