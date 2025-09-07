package org.example.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public final class PasswordUtil {   // final 붙이면 상속도 방지
    private static final SecureRandom RNG = new SecureRandom();

    // 🔒 인스턴스화를 막기 위한 private 생성자
    private PasswordUtil() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }

    public static String newSalt() {
        byte[] salt = new byte[16];
        RNG.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hash(String password, String base64Salt) {
        try {
            byte[] salt = Base64.getDecoder().decode(base64Salt);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashed = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new PasswordHashingException("SHA-256 algorithm not available", e);
        } catch (IllegalArgumentException e) {
            throw new PasswordHashingException("Invalid salt format", e);
        }
    }

    // 전용 예외 클래스
    public static class PasswordHashingException extends RuntimeException {
        public PasswordHashingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}