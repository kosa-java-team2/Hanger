package org.example.model;

import lombok.Getter;

/**
 * PostStatus
 * -------------------
 * 게시글(Post)의 거래 진행 상태를 표현하는 열거형.
 * <p>
 * 일반적인 전이(예시):
 *   ON_SALE → IN_PROGRESS → COMPLETED
 *   (취소/되돌리기 등은 서비스 정책에 맞게 별도 처리)
 * <p>
 * 설계/호환성 노트:
 * - 직렬화 저장을 고려해 **상수명 변경/삭제 금지**. 새로운 상태가 필요하면 상수를 추가하세요.
 * - enum의 **ordinal(순서) 의존 금지**: 비교/정렬은 명시적 Comparator나 맵핑 점수를 사용하세요.
 * - 현재 코드베이스(PostService 등)는 ON_SALE/IN_PROGRESS/COMPLETED를 사용합니다.
 *   SOLD는 프로젝트 스타일에 따라 사용할 수 있는 **선택적 상태**입니다(미사용이라면 정리 고려).
 * <p>
 * TradeStatus와의 관계:
 * - PostStatus는 "게시글"의 노출/진행 상태, TradeStatus는 "개별 거래"의 상태를 의미합니다.
 *   필요 시 상태 동기화를 위한 서비스 레이어 정책을 추가하세요.
 */
@Getter
public enum PostStatus {
    /** 판매중(기본) — 검색/노출 대상 */
    ON_SALE("판매중"),

    /** 거래중 — 예약/진행 등으로 신규 요청 제한(정책에 따라 다름) */
    IN_PROGRESS("거래중"),

    /**
     * 판매됨 — 게시글 차원에서 '판매 완료'를 뜻하는 중간 단계로 사용할 수 있음.
     * 프로젝트에 따라 COMPLETED와 중복될 수 있으니, 한쪽만 쓰는 일관된 정책 권장.
     */
    SOLD("판매됨"),

    /** 거래완료 — 거래가 확정 종료된 상태(노출 종료) */
    COMPLETED("거래완료");

    /**
     * -- GETTER --
     * 한글 라벨 반환
     */
    private final String label;

    PostStatus(String label) {
        this.label = label;
    }

}