package org.example.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * PasswordUtil
 * -------------------
 * 비밀번호 해싱과 salt 생성을 담당하는 유틸리티 클래스.
 *
 * 보안 노트(중요):
 * - 이 구현은 SHA-256 + salt 단발 해시를 사용합니다. 이는 학습/데모 용으로는 충분하지만,
 *   실제 운영 환경에서는 반드시 암호학적 KDF(키 유도 함수) 사용을 권장합니다.
 *   예) PBKDF2, bcrypt, scrypt, Argon2 (워크 팩터/메모리 코스트 설정)
 * - salt는 사용자마다 무작위로 달라야 하며, 별도의 보관 없이 해시와 함께 저장해도 됩니다.
 * - 추가 보안을 원한다면 서버 비밀키(pepper)를 적용하고, KDF의 반복 횟수를 점진적으로 상향하세요.
 *
 * 설계 노트:
 * - 유틸 클래스이므로 final + private 생성자로 인스턴스화를 방지합니다.
 * - 전역 SecureRandom 인스턴스를 사용(synchronized 불필요, thread-safe).
 * - 입력/출력은 Base64로 인코딩된 바이트를 문자열로 주고받습니다.
 */
public final class PasswordUtil {   // final 붙이면 상속도 방지
    /** 안전한 난수 생성을 위한 CSPRNG */
    private static final SecureRandom RNG = new SecureRandom();

    // 🔒 인스턴스화를 막기 위한 private 생성자
    private PasswordUtil() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }

    /**
     * 새로운 salt를 생성하여 Base64 문자열로 반환한다.
     *
     * 구현 세부:
     * - 16바이트(128비트) 무작위 salt 생성.
     * - 바이트 배열을 Base64로 인코딩해 보관/전달이 쉽도록 문자열로 반환.
     *
     * @return Base64 인코딩된 salt 문자열
     */
    public static String newSalt() {
        byte[] salt = new byte[16];     // 128-bit salt
        RNG.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * 비밀번호와 Base64 salt를 이용해 SHA-256 해시를 계산하고, 결과를 Base64로 반환한다.
     *
     * 절차:
     *  1) 전달받은 base64Salt를 디코딩하여 바이트 배열로 변환
     *  2) MessageDigest(SHA-256)에 salt를 먼저 update
     *  3) password의 UTF-8 바이트에 대해 digest 수행
     *  4) 결과 바이트를 Base64로 인코딩해 문자열 반환
     *
     * 주의:
     * - 운영 환경에서는 단발 해시 대신 KDF(PBKDF2/bcrypt/scrypt/Argon2) 사용을 권장.
     * - salt 포맷(Base64)이 잘못되면 IllegalArgumentException 이 발생할 수 있음.
     *
     * @param password   원문 비밀번호
     * @param base64Salt Base64 인코딩된 salt 문자열
     * @return Base64 인코딩된 해시 문자열
     * @throws PasswordHashingException SHA-256 미지원 또는 salt 포맷 오류 시 래핑 예외 발생
     */
    public static String hash(String password, String base64Salt) {
        try {
            byte[] salt = Base64.getDecoder().decode(base64Salt);  // Base64 → bytes
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt); // salt 먼저 혼합
            byte[] hashed = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            // 이론상 자바 표준에서 SHA-256은 항상 제공되지만, 방어적 처리
            throw new PasswordHashingException("SHA-256 algorithm not available", e);
        } catch (IllegalArgumentException e) {
            // Base64 디코딩 실패(잘못된 salt 포맷)
            throw new PasswordHashingException("Invalid salt format", e);
        }
    }

    /**
     * 비밀번호 해싱 관련 오류를 표현하는 런타임 예외.
     * - 호출 측에서는 선택적으로 잡아서 사용자 친화 메시지로 변환 가능.
     */
    public static class PasswordHashingException extends RuntimeException {
        public PasswordHashingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}