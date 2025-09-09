package org.example.model;

import lombok.Getter;

/**
 * ConditionLevel
 * -------------------
 * 상품의 "컨디션(상태)" 등급을 표현하는 열거형(Enum).
 * <p>
 * 사용 목적:
 * - {@link org.example.model.Post} 클래스의 상품 상태를 나타낼 때 활용된다.
 * - 사용자 입력("상/중/하")을 내부 enum 값(HIGH/MEDIUM/LOW)으로 변환하여 저장 및 비교.
 * - DB 또는 직렬화 시 "상/중/하" → enum → 문자열 변환을 통해 일관성 있는 데이터 관리가 가능하다.
 * <p>
 * 매핑 가이드:
 * - HIGH   ↔ "상" (최상급, 거의 새 상품 수준)
 * - MEDIUM ↔ "중" (보통, 일반적인 사용감)
 * - LOW    ↔ "하" (낮음, 사용감 많거나 흠집 있음)
 * <p>
 * 주의사항:
 * - enum의 순서(ordinal)에 의존하지 말 것.
 * 예: HIGH.ordinal() < MEDIUM.ordinal() 같은 비교는, 상수 정의 순서가 바뀌면 잘못된 의미가 된다.
 * 대신 별도의 점수(예: HIGH=3, MEDIUM=2, LOW=1)를 매핑하거나 Comparator를 작성하여 정렬/비교에 활용할 것.
 */
@Getter
public enum ConditionLevel {
    /**
     * HIGH ("상")
     * - 최상급 상태
     * - 사용감 거의 없음, 새 상품과 유사
     * - 예: 새 제품, 미개봉, 하자 없는 상품
     */
    HIGH("상"),

    /**
     * MEDIUM ("중")
     * - 중간 상태
     * - 일반적인 사용감 존재
     * - 예: 약간의 흠집, 사용 흔적은 있으나 정상 작동
     */
    MEDIUM("중"),

    /**
     * LOW ("하")
     * - 하급 상태
     * - 사용감 많거나 흠집/손상이 있는 상태
     * - 예: 오래된 제품, 외관 손상, 기능 일부 저하
     */
    LOW("하");

    // ===================== 필드 =====================
    /**
     * 한글 라벨 값
     * - 사용자에게 보여질 문자열("상", "중", "하")
     * - DB 저장 시에도 가독성을 위해 label을 그대로 사용할 수 있음
     */
    private final String label;

    // ===================== 생성자 =====================

    /**
     * 열거형 생성자
     *
     * @param label 한글 라벨 ("상/중/하")
     */
    ConditionLevel(String label) {
        this.label = label;
    }
}