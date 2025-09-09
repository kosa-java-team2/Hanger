package org.example.model;

import lombok.Getter;

/**
 * TradeStatus
 * -------------------
 * 개별 거래(Trade)의 수명주기 상태를 표현하는 열거형(Enum).
 * <p>
 * 대표 전이 흐름:
 * REQUESTED → ACCEPTED → IN_PROGRESS → COMPLETED
 * ↘─────────────┘
 * (언제든 CANCELLED로 종료 가능 — 정책에 따라 제한 가능)
 * <p>
 * 사용처:
 * - {@link org.example.model.Trade} 엔티티의 상태 필드
 * - {@link org.example.service.TradeService} 의 상태 변경/검증 로직
 * - 알림(Notification) 매핑 시 활용
 * (예: COMPLETED → NotificationType.TRADE_COMPLETED)
 * <p>
 * 설계/호환성 노트:
 * - DataStore 직렬화/스냅샷에 포함되므로 **상수명 변경/삭제 금지**
 * (변경 시 기존 저장 데이터와 호환성 깨짐)
 * - **ordinal(순서)에 의존 금지**
 * → 상태 비교/정렬 시 Comparator 또는 점수 매핑을 별도로 구현해야 함
 * - PostStatus와의 차이점:
 * - PostStatus: "게시글"의 노출/진행 상태 (판매중, 거래중, 거래완료 등)
 * - TradeStatus: "개별 거래"의 상세 진행 상태 (요청됨, 진행중, 완료됨 등)
 */
@Getter
public enum TradeStatus {
    /**
     * 요청됨 (REQUESTED)
     * - 구매자가 거래 요청을 보낸 직후 상태
     * - 초기 상태, 아직 판매자가 응답하지 않은 단계
     */
    REQUESTED("요청됨"),

    /**
     * 수락됨 (ACCEPTED)
     * - 판매자가 요청을 수락한 상태
     * - 일정/조건 조율 가능, 실제 거래 진행 준비 단계
     */
    ACCEPTED("수락됨"),

    /**
     * 진행중 (IN_PROGRESS)
     * - 거래가 실제로 진행 중인 상태
     * - 예: 직거래 약속, 송금/택배 발송 과정
     */
    IN_PROGRESS("진행중"),

    /**
     * 완료됨 (COMPLETED)
     * - 거래가 정상적으로 종료된 상태
     * - 상호 평가(buyer/sellerEvaluationGood)가 가능
     */
    COMPLETED("완료됨"),

    /**
     * 취소됨 (CANCELLED)
     * - 요청/수락/진행 상태 어디서든 취소 가능
     * - 일방 취소 또는 상호 합의 취소 모두 포함
     */
    CANCELLED("취소됨");

    // ===================== 필드 =====================
    /**
     * 한글 라벨 (UI 표시용)
     */
    private final String label;

    // ===================== 생성자 =====================
    TradeStatus(String label) {
        this.label = label;
    }
}