package org.example.util;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * PriceUtil
 * -------------------
 * 금액(정수)을 현지화된 숫자 문자열로 포맷하는 유틸리티.
 *
 * 특징:
 * - Locale.KOREA 기준으로 천 단위 구분(쉼표) 등을 적용한다.
 * - 통화 기호(₩)는 붙이지 않고 **숫자만** 포맷한다. (표시는 호출부에서 책임)
 *
 * 주의(중요):
 * - NumberFormat 인스턴스는 **스레드 세이프가 아니다**.
 *   여기서는 간단한 CLI/단일 스레드 시나리오를 가정해 static 공유 인스턴스를 사용한다.
 *   멀티스레드 환경이라면 아래 방법을 권장:
 *     1) 매 호출마다 NumberFormat.getInstance(...)로 새 인스턴스 생성, 또는
 *     2) ThreadLocal<NumberFormat>로 스레드별 인스턴스 보관.
 *
 * 한계:
 * - 파라미터가 int이므로 표현 가능한 범위를 초과할 수 있다.
 *   매우 큰 금액은 long/BigDecimal로 오버로드를 추가해 처리하는 것을 권장.
 */
public final class PriceUtil {   // final: 상속 방지
    /** 한국 로케일용 NumberFormat (단일 스레드 사용 가정) */
    private static final NumberFormat NF = NumberFormat.getInstance(Locale.KOREA);

    // 🔒 인스턴스화를 막는 private 생성자
    private PriceUtil() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }

    /**
     * 정수 금액을 한국 로케일 숫자 형식으로 포맷한다.
     * 예) 1000 -> "1,000"
     *
     * @param price 정수 금액(원)
     * @return 천 단위 구분이 적용된 문자열(통화 기호 없음)
     */
    public static String format(int price) {
        return NF.format(price);
    }
}