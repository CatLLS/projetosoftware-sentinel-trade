package com.sentineltrade.infrastructure.security;

import com.sentineltrade.config.AppProperties;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/** {secrecy}: encrypts a column at rest with AES-256-GCM. Stored as base64(iv || ciphertext). */
@Component
@Converter
public class SecretConverter implements AttributeConverter<String, String> {

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public SecretConverter(AppProperties properties) {
        String passphrase = properties.security().mfaEncryptionKey();
        if (passphrase == null || passphrase.isBlank()) {
            throw new IllegalStateException("MFA_SECRET_ENCRYPTION_KEY is not set");
        }
        try {
            byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                    .digest(passphrase.getBytes(StandardCharsets.UTF_8));
            this.key = new SecretKeySpec(keyBytes, "AES");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String convertToDatabaseColumn(String plain) {
        if (plain == null) {
            return null;
        }
        byte[] iv = new byte[IV_BYTES];
        random.nextBytes(iv);
        byte[] encrypted = run(Cipher.ENCRYPT_MODE, iv, plain.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length)
                .put(iv).put(encrypted).array());
    }

    @Override
    public String convertToEntityAttribute(String stored) {
        if (stored == null) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode(stored));
        byte[] iv = new byte[IV_BYTES];
        buffer.get(iv);
        byte[] encrypted = new byte[buffer.remaining()];
        buffer.get(encrypted);
        return new String(run(Cipher.DECRYPT_MODE, iv, encrypted), StandardCharsets.UTF_8);
    }

    private byte[] run(int mode, byte[] iv, byte[] input) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(mode, key, new GCMParameterSpec(TAG_BITS, iv));
            return cipher.doFinal(input);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Secret could not be processed", e);
        }
    }
}
