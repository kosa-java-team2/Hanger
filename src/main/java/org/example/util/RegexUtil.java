package org.example.util;

import java.util.regex.Pattern;

/**
 * RegexUtil
 * -------------------
 * 자주 사용하는 정규식(Regular Expression) 기반의 형식 검증 유틸리티 클래스.
 * <p>
 * 특징:
 * - 정규식 패턴을 미리 컴파일(Pattern.compile)하여 성능 최적화
 * - 모든 검증 메서드는 null 입력 방어 로직 포함
 * - 유틸리티 성격이므로 final 선언 + private 생성자
 */
public final class RegexUtil {

    /**
     * 사용자 ID: 영문 대소문자 또는 숫자만 허용, 길이 4~16자
     */
    private static final Pattern USER_ID = Pattern.compile("^[a-zA-Z0-9]{4,16}$");

    /**
     * 닉네임: 공백 불가, 길이 2~20자
     */
    private static final Pattern NICKNAME = Pattern.compile("^[^\\s]{2,20}$");

    /**
     * 주민등록번호(RRN): 앞 6자리-뒤 7자리 숫자 형식
     */
    private static final Pattern RRN = Pattern.compile("^\\d{6}-\\d{7}$");

    /**
     * 가격: 쉼표 포함 형식 (예: 1,000, 20,000,000)
     */
    private static final Pattern PRICE_COMMA = Pattern.compile("^\\d{1,3}(,\\d{3})*$");

    /**
     * 가격: 숫자만 (예: 1000, 20000)
     */
    private static final Pattern PRICE_PLAIN = Pattern.compile("^\\d+$");

    // 🔒 인스턴스화를 막는 private 생성자
    private RegexUtil() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }

    // ===================== 검증 메서드 =====================

    /**
     * 사용자 ID가 잘못된 경우 true 반환
     */
    public static boolean isInvalidUserId(String input) {
        return input == null || !USER_ID.matcher(input).matches();
    }

    /**
     * 닉네임이 잘못된 경우 true 반환
     */
    public static boolean isInvalidNickname(String input) {
        return input == null || !NICKNAME.matcher(input).matches();
    }

    /**
     * 주민등록번호가 잘못된 경우 true 반환
     */
    public static boolean isInvalidRRN(String input) {
        return input == null || !RRN.matcher(input).matches();
    }

    /**
     * 가격이 잘못된 경우 true 반환
     */
    public static boolean isInvalidPrice(String input) {
        return input == null ||
                !(PRICE_COMMA.matcher(input).matches() || PRICE_PLAIN.matcher(input).matches());
    }
}