package com.example.bankcards.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class CardEncryptor {

    private static final Logger logger = LoggerFactory.getLogger(CardEncryptor.class);

    private final SecretKeySpec secretKey;
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // Длина IV для GCM (в байтах)
    private static final int GCM_TAG_LENGTH = 16; // Длина тега аутентификации (в байтах)

    public CardEncryptor(@Value("${encryption.key:}") String key) {
        logger.debug("Initializing CardEncryptor with provided key length: {}", key.length());
        if (key == null || key.isEmpty()) {
            logger.error("Encryption key is not provided. Please set 'encryption.key' in application.properties or as an environment variable (ENCRYPTION_KEY).");
            throw new IllegalArgumentException("Encryption key is not provided. Please set 'encryption.key' in application.properties or as an environment variable.");
        }
        if (key.length() != 16) {
            logger.error("Encryption key must be exactly 16 characters long for AES-128, but was {} characters.", key.length());
            throw new IllegalArgumentException("Encryption key must be exactly 16 characters long for AES-128.");
        }
        this.secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        logger.info("CardEncryptor initialized successfully.");
    }

    public String encrypt(String data) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            byte[] encryptedWithIv = new byte[GCM_IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, GCM_IV_LENGTH);
            System.arraycopy(encrypted, 0, encryptedWithIv, GCM_IV_LENGTH, encrypted.length);

            String result = Base64.getEncoder().encodeToString(encryptedWithIv);
            logger.debug("Data encrypted successfully.");
            return result;
        } catch (Exception e) {
            logger.error("Encryption failed: {}", e.getMessage(), e);
            throw new RuntimeException("Encryption failed: " + e.getMessage(), e);
        }
    }

    public String decrypt(String encryptedData) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedData);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[decoded.length - GCM_IV_LENGTH];
            System.arraycopy(decoded, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(decoded, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            byte[] decrypted = cipher.doFinal(encrypted);
            String result = new String(decrypted, StandardCharsets.UTF_8);
            logger.debug("Data decrypted successfully.");
            return result;
        } catch (Exception e) {
            logger.error("Decryption failed: {}", e.getMessage(), e);
            throw new RuntimeException("Decryption failed: " + e.getMessage(), e);
        }
    }

    public String maskCardNumber(String encryptedCardNumber) {
        try {
            String decrypted = decrypt(encryptedCardNumber);
            if (decrypted.length() < 4) {
                logger.error("Card number too short to mask: length {}", decrypted.length());
                throw new IllegalArgumentException("Card number too short to mask");
            }
            String masked = decrypted.replaceAll(".{12}(.{4})", "**** **** **** $1");
            logger.debug("Card number masked successfully.");
            return masked;
        } catch (Exception e) {
            logger.error("Failed to mask card number: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to mask card number: " + e.getMessage(), e);
        }
    }
}