package com.sentineltrade.infrastructure.security;

import com.sentineltrade.domain.user.MfaVerifier;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** TOTP (RFC 6238): 6 digits, 30 s steps, accepting one step of clock drift. */
@Component
public class TotpMfaVerifier implements MfaVerifier {

    private static final int PERIOD_SECONDS = 30;
    private static final int ALLOWED_DRIFT_STEPS = 1;
    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private final Clock clock;

    @Autowired
    public TotpMfaVerifier() {
        this(Clock.systemUTC());
    }

    TotpMfaVerifier(Clock clock) {
        this.clock = clock;
    }

    @Override
    public boolean verify(String base32Secret, String code) {
        if (code == null || !code.matches("\\d{6}")) {
            return false;
        }
        long step = clock.instant().getEpochSecond() / PERIOD_SECONDS;
        boolean valid = false;
        for (long drift = -ALLOWED_DRIFT_STEPS; drift <= ALLOWED_DRIFT_STEPS; drift++) {
            // constant-time comparison, and no early exit
            valid |= MessageDigest.isEqual(codeAt(base32Secret, step + drift).getBytes(StandardCharsets.US_ASCII),
                    code.getBytes(StandardCharsets.US_ASCII));
        }
        return valid;
    }

    /** The code an authenticator app shows at the given instant. */
    public static String generate(String base32Secret, Instant at) {
        return codeAt(base32Secret, at.getEpochSecond() / PERIOD_SECONDS);
    }

    private static String codeAt(String base32Secret, long step) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(decodeBase32(base32Secret), "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(step).array());
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24) | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8) | (hash[offset + 3] & 0xff);
            return String.format("%06d", binary % 1_000_000);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] decodeBase32(String value) {
        var out = new ByteArrayOutputStream();
        int buffer = 0;
        int bits = 0;
        for (char c : value.replace("=", "").toUpperCase().toCharArray()) {
            int digit = BASE32.indexOf(c);
            if (digit < 0) {
                throw new IllegalArgumentException("Invalid base32 secret");
            }
            buffer = (buffer << 5) | digit;
            bits += 5;
            if (bits >= 8) {
                out.write((buffer >> (bits - 8)) & 0xff);
                bits -= 8;
            }
        }
        return out.toByteArray();
    }
}
