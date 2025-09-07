package org.example.util;

import java.util.regex.Pattern;

/**
 * RegexUtil
 * -------------------
 * 자주 사용하는 정규식 검증 패턴을 모아 둔 유틸리티 클래스.
 * <p>
 * 설계 노트:
 * - 성능을 위해 Pattern을 미리 컴파일해 static 상수로 보관한다(재사용 안전).
 * - java.util.regex.Pattern 은 스레드 세이프하게 재사용 가능하다.
 * - 이 클래스는 유틸리티 성격이므로 final 이며 인스턴스화를 막는다.
 * <p>
 * 입력 처리 권장:
 * - 호출 전 입력 문자열은 trim()으로 앞뒤 공백을 제거하고 넘기는 것을 권장한다.
 * - 이 유틸은 "형식"만 검증하며, 비즈니스 규칙(중복/금지어/범위 등)은 호출부에서 처리한다.
 * <p>
 * 주의(보안/개인정보):
 * - 주민등록번호(RRN)는 엄격한 보호 대상이다. 형식 검증 외에 저장·로그 노출을 피하고,
 *   가능한 암호화/마스킹/최소수집 원칙을 따를 것.
 */
public final class RegexUtil {   // final → 상속 방지

    /** 사용자 ID: 영문 대소문자/숫자만 허용, 길이 4~16 */
    private static final Pattern USER_ID = Pattern.compile("^[a-zA-Z0-9]{4,16}$");

    /**
     * 닉네임: 공백 문자를 포함하지 않는 2~20자
     * - [^\\s] : 모든 공백 문자(스페이스, 탭, 개행 등)를 제외
     * - 길이 제한 2~20
     */
    private static final Pattern NICKNAME = Pattern.compile("^[^\\s]{2,20}$");

    /**
     * 주민번호 형식: 6자리-7자리(숫자)
     * - 예: 000000-0000000
     * - 형식만 검증하며 유효 일자/검증코드/세대 구분 등 도메인 유효성은 포함하지 않음.
     */
    private static final Pattern RRN = Pattern.compile("^\\d{6}-\\d{7}$");

    /**
     * 가격(쉼표 포함 버전): 1~3자리 + (',' + 3자리) 반복
     * - 예: 1,000 / 12,345 / 123,456,789
     * - 잘못된 그룹(예: 12,34)은 거부
     */
    private static final Pattern PRICE_COMMA = Pattern.compile("^\\d{1,3}(,\\d{3})*$");

    /** 가격(숫자만): 하나 이상의 숫자 */
    private static final Pattern PRICE_PLAIN = Pattern.compile("^\\d+$");

    // 🔒 인스턴스화를 막는 private 생성자
    private RegexUtil() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }

    /**
     * 사용자 ID 형식 검증
     * 허용: [a-zA-Z0-9], 길이 4~16
     */
    public static boolean isValidUserId(String input) {
        return USER_ID.matcher(input).matches();
    }

    /**
     * 닉네임 형식 검증
     * 공백 문자를 포함하지 않는 2~20자
     */
    public static boolean isValidNickname(String input) {
        return NICKNAME.matcher(input).matches();
    }

    /**
     * 주민등록번호 형식 검증(YYYYMMDD-XXXXXXX 형태의 숫자 개수만 확인)
     * 실제 생년월일/성별 코드/체크섬 검사는 포함하지 않는다.
     */
    public static boolean isValidRRN(String input) {
        return RRN.matcher(input).matches();
    }

    /**
     * 가격 형식 검증
     * - 쉼표 포함 형식(PRICE_COMMA) 또는 숫자만(PRICE_PLAIN) 중 하나라도 만족하면 true
     * - 숫자 범위(최대/최소), 선행 0 허용 여부 등은 별도 정책으로 호출부에서 판단한다.
     */
    public static boolean isValidPriceWithCommaOrPlain(String input) {
        return PRICE_COMMA.matcher(input).matches() || PRICE_PLAIN.matcher(input).matches();
    }
}