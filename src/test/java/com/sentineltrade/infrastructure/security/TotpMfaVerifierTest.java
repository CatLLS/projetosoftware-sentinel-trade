package com.sentineltrade.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class TotpMfaVerifierTest {

    /** RFC 6238 test secret "12345678901234567890" in base32. */
    private static final String RFC_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    private static TotpMfaVerifier verifierAt(long epochSecond) {
        return new TotpMfaVerifier(Clock.fixed(Instant.ofEpochSecond(epochSecond), ZoneOffset.UTC));
    }

    @Test
    void matchesRfc6238TestVector() {
        // RFC 6238, appendix B: T = 59 s -> 94287082 (8 digits), i.e. 287082 with 6 digits
        assertThat(TotpMfaVerifier.generate(RFC_SECRET, Instant.ofEpochSecond(59))).isEqualTo("287082");
        assertThat(verifierAt(59).verify(RFC_SECRET, "287082")).isTrue();
    }

    @Test
    void acceptsOneStepOfClockDrift() {
        String code = TotpMfaVerifier.generate(RFC_SECRET, Instant.ofEpochSecond(1_000_000));
        assertThat(verifierAt(1_000_000 + 30).verify(RFC_SECRET, code)).isTrue();
        assertThat(verifierAt(1_000_000 - 30).verify(RFC_SECRET, code)).isTrue();
    }

    @Test
    void rejectsExpiredCode() {
        String code = TotpMfaVerifier.generate(RFC_SECRET, Instant.ofEpochSecond(1_000_000));
        assertThat(verifierAt(1_000_000 + 90).verify(RFC_SECRET, code)).isFalse();
    }

    @Test
    void rejectsMalformedCodes() {
        TotpMfaVerifier verifier = verifierAt(59);
        assertThat(verifier.verify(RFC_SECRET, null)).isFalse();
        assertThat(verifier.verify(RFC_SECRET, "28708")).isFalse();
        assertThat(verifier.verify(RFC_SECRET, "abcdef")).isFalse();
    }
}
