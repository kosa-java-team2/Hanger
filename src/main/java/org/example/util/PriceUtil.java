package org.example.util;

import java.text.NumberFormat;
import java.util.Locale;

public final class PriceUtil {   // final: 상속 방지
    private static final NumberFormat NF = NumberFormat.getInstance(Locale.KOREA);

    // 🔒 인스턴스화를 막는 private 생성자
    private PriceUtil() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }

    public static String format(int price) {
        return NF.format(price);
    }
}
