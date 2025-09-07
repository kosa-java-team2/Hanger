package org.example.model;

/**
 * ConditionLevel
 * -------------------
 * 상품의 컨디션(상태) 등급을 표현하는 열거형.
 * <p>
 * 사용처:
 * - {@link org.example.model.Post} 의 상품 상태(컨디션) 표기에 사용.
 * - 입력 값 "상/중/하" 를 내부 값 HIGH/MEDIUM/LOW 로 매핑하여 저장/비교.
 * <p>
 * 매핑 가이드:
 * - HIGH   ↔ "상"
 * - MEDIUM ↔ "중"
 * - LOW    ↔ "하"
 * <p>
 * 주의:
 * - enum의 순서(ordinal)에 의존한 정렬/비교는 지양하십시오.
 *   (예: HIGH.ordinal() < MEDIUM.ordinal() 와 같은 비교는, 상수 순서가 바뀔 경우
 *   의미가 깨질 수 있습니다. 필요 시 별도의 Comparator 또는 점수 매핑을 사용하세요.)
 */
public enum ConditionLevel {
    /** 상(최상급, 사용감 거의 없음) */
    HIGH,
    /** 중(보통, 일반적인 사용감) */
    MEDIUM,
    /** 하(낮음, 사용감 많음/흠집 등) */
    LOW
}