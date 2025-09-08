package org.example.model;

import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Trade 클래스
 * -------------------
 * 게시글(Post)을 기반으로 구매자와 판매자 간의 거래(Trade)를 표현하는 모델 클래스.
 * <p>
 * 특징:
 * - 직렬화 가능 (DataStore 저장/로드 지원)
 * - 거래 ID, 게시글 ID, 구매자/판매자, 상태, 생성/수정일시 등을 보관
 * - 상태 관리 메서드 제공 (REQUESTED → ACCEPTED → IN_PROGRESS → COMPLETED / CANCELLED)
 * - 상호 평가 시스템 지원 (buyerRatedGood, sellerRatedGood)
 */
@Getter
public class Trade implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // ===================== 필드 =====================

    // ===================== Getter 메서드 =====================
    /** 거래 고유 ID (DataStore에서 시퀀스를 통해 발급) */
    private final int tradeId;

    /** 거래 대상 게시글 ID */
    private final int relatedPostId;

    /** 구매자 ID */
    private final String buyerUserId;

    /** 판매자 ID */
    private final String sellerUserId;

    /** 거래 상태 (기본값: REQUESTED) */
    private TradeStatus tradeStatus = TradeStatus.REQUESTED;

    /** 거래 생성 시각 */
    private final LocalDateTime createdAt = LocalDateTime.now();

    /** 마지막 수정 시각 */
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * 구매자의 평가 여부
     * - null: 아직 평가하지 않음
     * - true: 좋은 평가(good)
     * - false: 나쁜 평가(bad)
     */
    private Boolean buyerEvaluationGood;

    /**
     * 판매자의 평가 여부 (선택적 사용)
     * - null: 아직 평가하지 않음
     * - true: 좋은 평가(good)
     * - false: 나쁜 평가(bad)
     */
    private Boolean sellerEvaluationGood;

    // ===================== 생성자 =====================
    /**
     * Trade 객체 생성자
     *
     * @param tradeId  거래 ID
     * @param relatedPostId   거래 대상 게시글 ID
     * @param buyerUserId  구매자 ID
     * @param sellerUserId 판매자 ID
     */
    public Trade(int tradeId, int relatedPostId, String buyerUserId, String sellerUserId) {
        this.tradeId = tradeId;
        this.relatedPostId = relatedPostId;
        this.buyerUserId = buyerUserId;
        this.sellerUserId = sellerUserId;
    }

    // ===================== 상태 변경 메서드 =====================
    /** 거래 상태를 직접 지정 */
    public void updateTradeStatus(TradeStatus newStatus) {
        this.tradeStatus = newStatus;
        refreshUpdatedAt();
    }

    /** 거래 수락 (REQUESTED → ACCEPTED) */
    public void acceptTrade() {
        this.tradeStatus = TradeStatus.ACCEPTED;
        refreshUpdatedAt();
    }

    /** 거래 진행 중으로 변경 (ACCEPTED → IN_PROGRESS) */
    public void startTradeProgress() {
        this.tradeStatus = TradeStatus.IN_PROGRESS;
        refreshUpdatedAt();
    }

    /** 거래 완료 (IN_PROGRESS → COMPLETED) */
    public void completeTrade() {
        this.tradeStatus = TradeStatus.COMPLETED;
        refreshUpdatedAt();
    }

    /** 거래 취소 (REQUESTED/ACCEPTED/IN_PROGRESS → CANCELLED) */
    public void cancelTrade() {
        this.tradeStatus = TradeStatus.CANCELLED;
        refreshUpdatedAt();
    }

    // ===================== 평가 메서드 =====================
    /**
     * 구매자가 판매자에 대해 평가 (good/bad)
     * @param isGood true=good, false=bad
     */
    public void evaluateByBuyer(boolean isGood) {
        this.buyerEvaluationGood = isGood;
        refreshUpdatedAt();
    }

    /**
     * 판매자가 구매자에 대해 평가 (good/bad)
     * @param isGood true=good, false=bad
     */
    public void evaluateBySeller(boolean isGood) {
        this.sellerEvaluationGood = isGood;
        refreshUpdatedAt();
    }

    // ===================== 내부 헬퍼 =====================
    /** updatedAt을 현재 시각으로 갱신 */
    private void refreshUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }

    // ===================== toString =====================
    /**
     * 거래 정보를 사람이 읽기 쉬운 문자열로 반환
     * 형식: Trade[ID] post=게시글ID buyer=구매자 seller=판매자 status=상태
     */
    @Override
    public String toString() {
        return String.format("Trade[%d] post=%d buyer=%s seller=%s status=%s",
                tradeId, relatedPostId, buyerUserId, sellerUserId, tradeStatus);
    }
}