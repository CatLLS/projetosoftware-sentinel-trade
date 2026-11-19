package com.sentineltrade.domain.user;

/** Port for second-factor verification, decoupled from the algorithm (TOTP, SMS, ...). */
public interface MfaVerifier {

    boolean verify(String secret, String code);
}
