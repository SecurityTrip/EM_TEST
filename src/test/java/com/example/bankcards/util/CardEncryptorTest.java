package com.example.bankcards.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CardEncryptorTest {

    @Test
    void encrypt_decrypt_and_mask() {
        CardEncryptor enc = new CardEncryptor("1234567890ABCDEF");
        String num = "1234567812345678";
        String encrypted = enc.encrypt(num);
        Assertions.assertNotEquals(num, encrypted);
        String decrypted = enc.decrypt(encrypted);
        Assertions.assertEquals(num, decrypted);
        String masked = enc.maskCardNumber(encrypted);
        Assertions.assertEquals("**** **** **** 5678", masked);
    }
}


