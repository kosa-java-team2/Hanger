package org.example.model;

/**
 * TradeStatus
 * -------------------
 * 개별 거래(Trade)의 수명주기 상태를 표현하는 열거형.
 *
 * 대표 전이(예시):
 *   REQUESTED → ACCEPTED → IN_PROGRESS → COMPLETED
 *                         ↘─────────────┘
 *   (언제든 CANCELLED 로 종료 가능 — 정책에 따라 제한)
 *
 * 사용처:
 * - {@link org.example.model.Trade} 엔티티의 상태 필드
 * - {@link org.example.service.TradeService} 상태 변경/검증 로직
 * - 알림 타입 매핑(예: COMPLETED → NotificationType.TRADE_COMPLETED 등)
 *
 * 설계/호환성 노트:
 * - 직렬화/스냅샷과 연동되므로 **상수명 변경/삭제 금지**(기존 데이터 호환성 깨짐).
 * - **ordinal(순서) 의존 금지**: 정렬/우선순위는 명시적 Comparator 또는 점수 매핑 사용.
 * - PostStatus는 "게시글"의 상태, TradeStatus는 "거래 건"의 상태로 역할이 다름.
 */
public enum TradeStatus {
    /** 요청됨(대기 상태): 구매자가 거래 요청을 보낸 직후 */
    REQUESTED,

    /** 수락됨: 판매자가 요청을 수락, 일정/조건 조율 단계로 진입 가능 */
    ACCEPTED,

    /** 거래 진행중: 실물 거래/송금/택배 진행 등 실질적 처리 단계 */
    IN_PROGRESS,

    /** 거래 완료: 거래가 정상적으로 종료됨(평가 가능) */
    COMPLETED,

    /** 취소됨: 한쪽 또는 합의에 의해 거래 중단 */
    CANCELLED
}