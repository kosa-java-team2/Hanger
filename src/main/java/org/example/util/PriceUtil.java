package org.example.util;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * PriceUtil
 * -------------------
 * 금액(정수)을 현지화된 숫자 문자열로 변환하는 유틸리티 클래스.
 * <p>
 * 특징:
 * - Locale.KOREA 기준으로 숫자 포맷팅 수행
 * → 천 단위 구분 기호(,) 적용
 * → 통화 기호(₩)는 붙이지 않고 **숫자만** 반환
 * - 통화 단위나 기호를 붙이는 책임은 호출부에서 처리
 * <p>
 * 주의 사항:
 * - NumberFormat 인스턴스는 스레드 세이프하지 않음
 * → 현재 구현은 단일 스레드/CLI 환경 가정하에 static 공유 인스턴스를 사용
 * → 멀티스레드 환경이라면 아래 방법 중 하나를 권장:
 * 1) 매 호출마다 NumberFormat.getInstance(Locale.KOREA)로 새 인스턴스 생성
 * 2) ThreadLocal<NumberFormat>로 스레드별 인스턴스 관리
 * <p>
 * 한계:
 * - format 메서드의 파라미터 타입이 int이므로 최대 약 21억(2,147,483,647)까지만 표현 가능
 * - 매우 큰 금액(long, BigDecimal 등)이 필요하다면 오버로드 메서드를 추가하는 것이 바람직함
 */
public final class PriceUtil {   // final: 상속 방지
    /**
     * 한국 로케일에 맞춘 NumberFormat 인스턴스 (단일 스레드 사용 가정)
     */
    private static final NumberFormat NF = NumberFormat.getInstance(Locale.KOREA);

    // 🔒 인스턴스화를 막는 private 생성자
    private PriceUtil() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }

    /**
     * 정수 금액을 한국 로케일 숫자 형식으로 변환
     * - 천 단위 구분(쉼표 ,) 적용
     * - 통화 기호(₩)는 포함하지 않음
     * <p>
     * 사용 예:
     * PriceUtil.format(1000) → "1,000"
     *
     * @param price 정수 금액(원 단위)
     * @return 천 단위 구분이 적용된 문자열 (숫자만, 통화 기호 없음)
     */
    public static String format(int price) {
        return NF.format(price);
    }
}