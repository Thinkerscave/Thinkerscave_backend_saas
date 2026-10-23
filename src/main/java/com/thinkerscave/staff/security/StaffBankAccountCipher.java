package com.thinkerscave.staff.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM cipher for staff bank account numbers.
 * Key: base64-encoded 32 bytes from environment variable STAFF_BANK_ENC_KEY.
 * Output format: base64(iv[12] || ciphertext+tag).
 * Fails closed: when the key is missing or malformed, encryption throws and nothing is stored.
 */
@Component
public class StaffBankAccountCipher {

    public static final int KEY_VERSION = 1;
    static final int KEY_BYTES = 32;
    static final int IV_BYTES = 12;
    static final int TAG_BITS = 128;

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private final String configuredKey;
    private final SecureRandom random = new SecureRandom();

    public StaffBankAccountCipher(@Value("${STAFF_BANK_ENC_KEY:}") String configuredKey) {
        this.configuredKey = configuredKey;
    }

    public boolean isConfigured() {
        return StringUtils.hasText(configuredKey);
    }

    public String encrypt(String plaintext) {
        SecretKey key = requireKey();
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] out = ByteBuffer.allocate(iv.length + ciphertext.length).put(iv).put(ciphertext).array();
            return Base64.getEncoder().encodeToString(out);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Bank account encryption failed", ex);
        }
    }

    public String decrypt(String encoded) {
        SecretKey key = requireKey();
        try {
            byte[] in = Base64.getDecoder().decode(encoded);
            if (in.length <= IV_BYTES) {
                throw new IllegalStateException("Encrypted bank account payload is malformed");
            }
            ByteBuffer buf = ByteBuffer.wrap(in);
            byte[] iv = new byte[IV_BYTES];
            buf.get(iv);
            byte[] ciphertext = new byte[buf.remaining()];
            buf.get(ciphertext);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), java.nio.charset.StandardCharsets.UTF_8);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Bank account decryption failed", ex);
        }
    }

    private SecretKey requireKey() {
        if (!isConfigured()) {
            throw new IllegalStateException(
                    "STAFF_BANK_ENC_KEY is not configured; bank account numbers cannot be stored");
        }
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(configuredKey.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("STAFF_BANK_ENC_KEY must be base64-encoded", ex);
        }
        if (raw.length != KEY_BYTES) {
            throw new IllegalStateException("STAFF_BANK_ENC_KEY must decode to exactly 32 bytes");
        }
        return new SecretKeySpec(raw, "AES");
    }
}
