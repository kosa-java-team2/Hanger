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
 * - 상호 평가 시스템 지원 (buyerEvaluationGood, sellerEvaluationGood)
 * <p>
 * 설계 의도:
 * - PostStatus(게시글 상태)와는 별도로, TradeStatus(개별 거래 상태)를 분리 관리
 * - 한 게시글(Post)에서 여러 Trade가 생길 수 있음
 * - 거래 단계 전환(accept, start, complete, cancel)은 명시적 메서드 호출로만 가능
 */
@Getter
public class Trade implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // ===================== 필드 =====================

    /**
     * 거래 고유 ID
     * - DataStore의 nextTradeId() 시퀀스를 통해 발급
     * - Trade의 Primary Key 역할
     */
    private final int tradeId;

    /**
     * 거래 대상 게시글 ID
     * - 어느 게시글(Post)을 기반으로 거래가 생성되었는지 추적
     */
    private final int relatedPostId;

    /**
     * 구매자 ID (User.id 참조)
     */
    private final String buyerUserId;

    /**
     * 판매자 ID (User.id 참조)
     */
    private final String sellerUserId;

    /**
     * 거래 상태
     * - 기본값: REQUESTED (거래 요청됨)
     * - 상태 전환: REQUESTED → ACCEPTED → IN_PROGRESS → COMPLETED
     * - 예외적으로 CANCELLED로 언제든지 종료 가능
     */
    private TradeStatus tradeStatus = TradeStatus.REQUESTED;

    /**
     * 거래 생성 시각 (객체 생성 시 자동 기록)
     */
    private final LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 마지막 수정 시각 (상태 변경, 평가 시마다 갱신됨)
     */
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * 구매자의 평가 여부
     * - null: 아직 평가하지 않음
     * - true: 좋은 평가(good)
     * - false: 나쁜 평가(bad)
     * <p>
     * → 거래가 완료되었을 때 신뢰도(TrustScore)에 반영 가능
     */
    private Boolean buyerEvaluationGood;

    /**
     * 판매자의 평가 여부
     * - null: 아직 평가하지 않음
     * - true: 좋은 평가(good)
     * - false: 나쁜 평가(bad)
     * <p>
     * → 선택적 사용 (프로젝트 정책에 따라 생략 가능)
     */
    private Boolean sellerEvaluationGood;

    // ===================== 생성자 =====================

    /**
     * Trade 객체 생성자
     *
     * @param tradeId       거래 ID (DataStore 발급)
     * @param relatedPostId 거래 대상 게시글 ID
     * @param buyerUserId   구매자 ID
     * @param sellerUserId  판매자 ID
     *                      <p>
     *                      생성 시 tradeStatus는 REQUESTED, createdAt/updatedAt은 현재 시각으로 초기화된다.
     */
    public Trade(int tradeId, int relatedPostId, String buyerUserId, String sellerUserId) {
        this.tradeId = tradeId;
        this.relatedPostId = relatedPostId;
        this.buyerUserId = buyerUserId;
        this.sellerUserId = sellerUserId;
    }

    // ===================== 상태 변경 메서드 =====================

    /**
     * 거래 수락
     * - 상태 전환: REQUESTED → ACCEPTED
     * - 보통 판매자가 거래 요청을 수락할 때 호출
     */
    public void acceptTrade() {
        this.tradeStatus = TradeStatus.ACCEPTED;
        refreshUpdatedAt();
    }

    /**
     * 거래 진행 시작
     * - 상태 전환: ACCEPTED → IN_PROGRESS
     * - 거래가 실제로 이루어지는 단계
     */
    public void startTradeProgress() {
        this.tradeStatus = TradeStatus.IN_PROGRESS;
        refreshUpdatedAt();
    }

    /**
     * 거래 완료
     * - 상태 전환: IN_PROGRESS → COMPLETED
     * - 물품/대금이 정상적으로 교환되었을 때 호출
     */
    public void completeTrade() {
        this.tradeStatus = TradeStatus.COMPLETED;
        refreshUpdatedAt();
    }

    /**
     * 거래 취소
     * - 상태 전환: REQUESTED/ACCEPTED/IN_PROGRESS → CANCELLED
     * - 어느 단계에서든 취소 가능 (정책에 따라 제한 가능)
     */
    public void cancelTrade() {
        this.tradeStatus = TradeStatus.CANCELLED;
        refreshUpdatedAt();
    }

    // ===================== 평가 메서드 =====================

    /**
     * 구매자가 판매자를 평가
     *
     * @param isGood true=좋은 평가, false=나쁜 평가
     *               - 보통 거래 완료 후 호출
     *               - 신뢰도(TrustScore)에 반영 가능
     */
    public void evaluateByBuyer(boolean isGood) {
        this.buyerEvaluationGood = isGood;
        refreshUpdatedAt();
    }

    /**
     * 판매자가 구매자를 평가
     *
     * @param isGood true=좋은 평가, false=나쁜 평가
     *               - 선택적 사용
     */
    public void evaluateBySeller(boolean isGood) {
        this.sellerEvaluationGood = isGood;
        refreshUpdatedAt();
    }

    // ===================== 내부 헬퍼 =====================

    /**
     * updatedAt을 현재 시각으로 갱신
     */
    private void refreshUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }

    // ===================== toString =====================

    /**
     * 거래 요약 정보를 문자열로 반환
     * <p>
     * 출력 형식:
     * Trade[ID] post=게시글ID buyer=구매자ID seller=판매자ID status=상태
     * <p>
     * 예시:
     * Trade[2001] post=1001 buyer=userA seller=userB status=IN_PROGRESS
     */
    @Override
    public String toString() {
        return String.format("Trade[%d] post=%d buyer=%s seller=%s status=%s",
                tradeId, relatedPostId, buyerUserId, sellerUserId, tradeStatus);
    }
}