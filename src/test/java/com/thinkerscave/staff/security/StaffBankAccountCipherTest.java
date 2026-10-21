package com.thinkerscave.staff.security;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class StaffBankAccountCipherTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    void roundTripsAccountNumberAndNeverStoresPlaintext() {
        StaffBankAccountCipher cipher = new StaffBankAccountCipher(KEY);
        String encrypted = cipher.encrypt("123456789012");
        assertFalse(encrypted.contains("123456789012"));
        assertEquals("123456789012", cipher.decrypt(encrypted));
    }

    @Test
    void producesDifferentCiphertextForSamePlaintext() {
        StaffBankAccountCipher cipher = new StaffBankAccountCipher(KEY);
        assertNotEquals(cipher.encrypt("123456"), cipher.encrypt("123456"));
    }

    @Test
    void failsClosedWhenKeyMissing() {
        StaffBankAccountCipher cipher = new StaffBankAccountCipher("");
        assertFalse(cipher.isConfigured());
        assertThrows(IllegalStateException.class, () -> cipher.encrypt("123456"));
    }

    @Test
    void rejectsKeyOfWrongLength() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);
        StaffBankAccountCipher cipher = new StaffBankAccountCipher(shortKey);
        assertThrows(IllegalStateException.class, () -> cipher.encrypt("123456"));
    }

    @Test
    void rejectsTamperedCiphertext() {
        StaffBankAccountCipher cipher = new StaffBankAccountCipher(KEY);
        byte[] raw = Base64.getDecoder().decode(cipher.encrypt("123456"));
        raw[raw.length - 1] ^= 0x01;
        String tampered = Base64.getEncoder().encodeToString(raw);
        assertThrows(IllegalStateException.class, () -> cipher.decrypt(tampered));
    }
}
