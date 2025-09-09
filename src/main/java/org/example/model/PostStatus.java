package org.example.model;

import lombok.Getter;

/**
 * PostStatus
 * -------------------
 * 게시글(Post)의 거래 진행 상태를 표현하는 열거형(Enum).
 * <p>
 * 사용 목적:
 * - 판매 게시글이 현재 어떤 단계에 있는지를 명확하게 표현한다.
 * - 서비스 로직, UI 표시, 검색 필터링 등에 활용된다.
 * <p>
 * 일반적인 상태 전이 예시:
 * ON_SALE(판매중) → IN_PROGRESS(거래중) → COMPLETED(거래완료)
 * (취소/되돌리기 등은 서비스 정책에 따라 별도 처리)
 * <p>
 * 설계/호환성 노트:
 * - DataStore 직렬화를 고려해 **상수명 변경/삭제 금지**.
 * 새로운 상태가 필요하면 상수를 추가해야 함.
 * - enum의 **ordinal(순서)에 의존한 정렬/비교 금지**.
 * → 필요하면 Comparator 또는 점수 매핑 방식을 활용.
 * - 현재 코드(PostService 등)에서는 ON_SALE, IN_PROGRESS, COMPLETED를 주로 사용.
 * SOLD 같은 상태는 프로젝트 정책에 따라 선택적으로 활용하거나 정리 가능.
 * <p>
 * TradeStatus와의 관계:
 * - PostStatus는 "게시글"의 노출/진행 상태를 표현.
 * - TradeStatus는 "개별 거래"의 상세 상태를 표현.
 * → 서로 다른 개념이므로 필요 시 서비스 계층에서 동기화 정책을 추가해야 함.
 */
@Getter
public enum PostStatus {
    /**
     * 판매중 상태 (기본값)
     * - 검색/노출 대상
     * - 누구나 거래 요청 가능
     */
    ON_SALE("판매중"),

    /**
     * 거래중 상태
     * - 이미 예약되었거나 진행 중이라 신규 요청이 제한되는 상태
     * - 정책에 따라 "예약중"과 "진행중"을 구분할 수도 있음
     */
    IN_PROGRESS("거래중"),

    /**
     * 거래완료 상태
     * - 거래가 확정 종료됨
     * - 더 이상 노출/검색되지 않음
     */
    COMPLETED("거래완료");

    // ===================== 필드 =====================
    /**
     * 상태를 나타내는 한글 라벨
     * - UI 표시 및 사용자 메시지에 사용
     * - 예: "판매중", "거래중", "거래완료"
     */
    private final String label;

    // ===================== 생성자 =====================

    /**
     * PostStatus 생성자
     *
     * @param label 한글 라벨
     */
    PostStatus(String label) {
        this.label = label;
    }
}