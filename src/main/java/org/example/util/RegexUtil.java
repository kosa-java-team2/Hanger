package org.example.util;

import java.util.regex.Pattern;

public final class RegexUtil {   // final → 상속 방지
    private static final Pattern USER_ID = Pattern.compile("^[a-zA-Z0-9]{4,16}$");
    private static final Pattern NICKNAME = Pattern.compile("^[^\\s]{2,20}$"); // 공백 불가
    private static final Pattern RRN = Pattern.compile("^\\d{6}-\\d{7}$");
    private static final Pattern PRICE_COMMA = Pattern.compile("^\\d{1,3}(,\\d{3})*$");
    private static final Pattern PRICE_PLAIN = Pattern.compile("^\\d+$");

    // 🔒 인스턴스화를 막는 private 생성자
    private RegexUtil() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }

    public static boolean isValidUserId(String s) {
        return USER_ID.matcher(s).matches();
    }

    public static boolean isValidNickname(String s) {
        return NICKNAME.matcher(s).matches();
    }

    public static boolean isValidRRN(String s) {
        return RRN.matcher(s).matches();
    }

    public static boolean isValidPriceWithCommaOrPlain(String s) {
        return PRICE_COMMA.matcher(s).matches() || PRICE_PLAIN.matcher(s).matches();
    }
}