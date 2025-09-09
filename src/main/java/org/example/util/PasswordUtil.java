package org.example.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * PasswordUtil
 * -------------------
 * 비밀번호 해싱과 salt 생성 기능을 제공하는 유틸리티 클래스.
 * <p>
 * 보안 관련 주의:
 * - 본 구현은 SHA-256 + salt 단일 해시 방식을 사용 (학습/데모용으로 적합)
 * - 실제 운영 환경에서는 반드시 KDF(키 유도 함수)를 사용해야 함:
 * · PBKDF2, bcrypt, scrypt, Argon2 등
 * · 반복 횟수(워크 팩터), 메모리 비용 등을 조정 가능
 * - salt는 사용자마다 무작위 생성되어야 하며, 해시와 함께 저장해도 안전함
 * - 추가적으로 pepper(서버 비밀 키)를 적용하면 더 강력한 보안 확보 가능
 * <p>
 * 설계:
 * - final 클래스: 상속 불가
 * - private 생성자: 인스턴스화 방지
 * - SecureRandom 인스턴스를 전역으로 유지 (thread-safe)
 * - 모든 입출력은 Base64 인코딩 문자열로 다룸 (바이너리 안전)
 */
public final class PasswordUtil {   // final: 상속 불가
    /**
     * 안전한 난수 생성을 위한 SecureRandom 인스턴스
     */
    private static final SecureRandom RNG = new SecureRandom();

    // 🔒 외부에서 인스턴스화를 막기 위한 private 생성자
    private PasswordUtil() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }

    // ===================== Salt 생성 =====================

    /**
     * 새로운 salt를 생성하여 Base64 문자열로 반환
     * <p>
     * 동작 방식:
     * - 16바이트(128비트) 길이의 난수 생성
     * - 생성된 바이트 배열을 Base64로 인코딩 후 반환
     *
     * @return Base64 인코딩된 salt 문자열
     */
    public static String newSalt() {
        byte[] salt = new byte[16];     // 128-bit salt
        RNG.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    // ===================== 비밀번호 해싱 =====================

    /**
     * 비밀번호와 salt를 조합하여 SHA-256 해시를 계산
     * <p>
     * 처리 절차:
     * 1) 전달받은 base64Salt를 디코딩하여 바이트 배열로 변환
     * 2) MessageDigest(SHA-256) 초기화 후 salt 바이트 배열을 update
     * 3) 비밀번호를 UTF-8 바이트 배열로 변환하여 digest 수행
     * 4) 결과 바이트 배열을 Base64로 인코딩해 문자열 반환
     * <p>
     * 주의:
     * - SHA-256 단일 해시는 운영 환경에서 안전하지 않음 → KDF 사용 권장
     * - 잘못된 Base64 salt 문자열이 들어오면 IllegalArgumentException 발생 가능
     *
     * @param password   원문 비밀번호
     * @param base64Salt Base64 인코딩된 salt 문자열
     * @return Base64 인코딩된 해시 문자열
     * @throws PasswordHashingException SHA-256 미지원 또는 salt 포맷 오류 시 래핑 예외 발생
     */
    public static String hash(String password, String base64Salt) {
        try {
            byte[] salt = Base64.getDecoder().decode(base64Salt);  // Base64 → 바이트 배열
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt); // salt 먼저 반영
            byte[] hashed = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            // 일반적으로 자바 표준에서 SHA-256은 항상 지원되지만, 방어적 처리
            throw new PasswordHashingException("SHA-256 algorithm not available", e);
        } catch (IllegalArgumentException e) {
            // Base64 디코딩 실패 → 잘못된 salt 포맷
            throw new PasswordHashingException("Invalid salt format", e);
        }
    }

    // ===================== 커스텀 예외 =====================

    /**
     * 비밀번호 해싱 과정에서 발생하는 오류를 감싸는 런타임 예외 클래스
     * - 호출 측에서 잡아 사용자 친화적인 메시지로 변환 가능
     */
    public static class PasswordHashingException extends RuntimeException {
        public PasswordHashingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}