package org.example.service;

import org.example.datastore.DataStore;
import org.example.model.Notification;
import org.example.model.NotificationType;
import org.example.model.Post;
import org.example.model.PostStatus;
import org.example.model.Trade;
import org.example.model.TradeStatus;
import org.example.model.User;
import org.example.util.InputUtil;
import org.example.util.SortUtil;

import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 * TradeService
 * -------------------
 * 거래(Trade) 수명주기 전반을 다루는 서비스 레이어.
 * <p>
 * 주요 역할:
 * - requestTrade: 게시글 상세 화면에서 구매자가 거래 요청 생성
 * - manageTrades: 내 거래 목록을 조회하고 상태 변경/평가 처리
 * <p>
 * 설계 원칙:
 * - 모든 데이터는 DataStore를 통해 접근/수정/저장
 * - 상태 변경/평가 후에는 항상 store.saveToDisk() 호출하여 스냅샷 저장
 * - Trade 상태와 연관된 Post 상태는 항상 동기화 유지
 * - 거래 관련 이벤트(요청/상태 변경/완료)는 알림(Notification)으로 기록
 */
public record TradeService(DataStore store) {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ===================== 거래 요청 =====================

    /**
     * requestTrade
     * - 게시글 상세 화면에서 구매자가 거래 요청을 보낼 때 호출
     * <p>
     * 처리 흐름:
     * 1) 구매자와 판매자가 동일인일 경우 → 요청 불가
     * 2) 새로운 tradeId 발급 → Trade 객체 생성 → trades 맵에 저장
     * 3) 판매자에게 거래 요청 알림(Notification) 생성 및 저장
     * 4) store.saveToDisk()로 데이터 반영
     */
    public void requestTrade(User buyer, Post post) {
        if (buyer.getId().equals(post.getSellerId())) {
            System.out.println("본인 게시글에는 거래 요청을 할 수 없습니다.");
            return;
        }
        int tradeId = store.nextTradeId();
        Trade trade = new Trade(tradeId, post.getPostId(), buyer.getId(), post.getSellerId());
        store.trades().put(tradeId, trade);

        int notificationId = store.nextNotificationId();
        Notification notification = new Notification(
                notificationId,
                post.getSellerId(),
                NotificationType.TRADE_REQUEST,
                String.format("%s 님이 [%d] %s 거래를 요청했습니다.", buyer.getNickname(), post.getPostId(), post.getTitle())
        );
        store.notifications().put(notificationId, notification);

        store.saveToDisk();
        System.out.println("거래 요청이 전송되었습니다.");
    }

    // ===================== 거래 관리(진입점) =====================

    /**
     * manageTrades
     * - 내 거래 관리 메뉴 진입점
     * <p>
     * 처리 흐름:
     * 1) 현재 사용자가 buyer/seller로 참여한 거래 목록 조회
     * 2) 거래 내역 출력
     * 3) 메뉴 선택 (0=뒤로, 1=상태 변경, 2=평가)
     */
    public void manageTrades(User currentUser) {
        System.out.println("====== 내 거래 ======");
        List<Trade> myTrades = loadUserTrades(currentUser);

        if (myTrades.isEmpty()) {
            System.out.println("거래 내역이 없습니다.");
            return;
        }
        renderTrades(myTrades);

        switch (readMainAction()) {
            case 0:
                System.out.println("뒤로 이동합니다.");
                break;
            case 1:
                handleStatusChangeFlow(currentUser);
                break;
            case 2:
                handleEvaluationFlow(currentUser);
                break;
            default:
                System.out.println("잘못된 선택입니다.");
                break;
        }
    }

    // ===================== 조회/렌더/입력 =====================

    /**
     * loadUserTrades
     * - 현재 사용자가 buyer 또는 seller로 참여한 거래만 필터링
     * - ID 오름차순 정렬 후 반환
     */
    private List<Trade> loadUserTrades(User currentUser) {
        List<Trade> result = new ArrayList<>();
        for (Trade trade : store.trades().values()) {
            if (currentUser.getId().equals(trade.getBuyerUserId()) || currentUser.getId().equals(trade.getSellerUserId())) {
                result.add(trade);
            }
        }
        SortUtil.sortTradesById(result);
        return result;
    }

    /**
     * renderTrades
     * - 거래 목록을 콘솔에 출력
     * - 거래 ID, 관련 게시글, 구매자/판매자 ID, 거래 상태, 생성/수정일 출력
     * - 상태 변경/평가 메뉴 안내
     */
    private void renderTrades(List<Trade> trades) {
        for (Trade trade : trades) {
            String createdAt = trade.getCreatedAt() != null ? trade.getCreatedAt().format(DATE_FORMATTER) : "";
            String updatedAt = trade.getUpdatedAt() != null ? trade.getUpdatedAt().format(DATE_FORMATTER) : "";

            System.out.printf(
                    "[%d] 게시글=%d  구매자=%s  판매자=%s  상태=%s  (생성=%s ~ 수정=%s)%n",
                    trade.getTradeId(),
                    trade.getRelatedPostId(),
                    trade.getBuyerUserId(),
                    trade.getSellerUserId(),
                    trade.getTradeStatus().getLabel(),
                    createdAt,
                    updatedAt
            );
        }
        System.out.println("1. 거래 상태 변경  2. 거래 평가  0. 뒤로");
    }

    /**
     * 메인 액션 선택 입력 (0=뒤로, 1=상태 변경, 2=평가)
     */
    private int readMainAction() {
        return InputUtil.readIntInRange("선택: ", 0, 2);
    }

    // ===================== 상태 변경 플로우 =====================

    /**
     * handleStatusChangeFlow
     * - 거래 상태 변경 절차
     * <p>
     * 처리 흐름:
     * 1) 거래 ID 입력 → 본인 거래인지 확인
     * 2) 판매자만 변경 가능
     * 3) 새 상태 입력 (ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED)
     * 4) Trade 상태 변경
     * 5) 연관 Post 상태 동기화
     * 6) 상대방에게 알림 발송
     * 7) 저장
     */
    private void handleStatusChangeFlow(User currentUser) {
        int tradeId = InputUtil.readInt("변경할 거래 ID: ");
        Trade trade = store.trades().get(tradeId);
        if (!validateIsMyTradeOrWarn(currentUser, trade)) return;

        // 판매자만 거래 상태 변경 가능
        if (!currentUser.getId().equals(trade.getSellerUserId())) {
            System.out.println("거래 상태는 판매자만 변경할 수 있습니다.");
            return;
        }

        TradeStatus newStatus = readTradeStatus();
        if (newStatus == null) {
            System.out.println("잘못된 선택");
            return;
        }

        applyStatusChange(trade, newStatus);
        syncRelatedPostStatus(trade, newStatus);
        notifyCounterpartyOnStatus(currentUser, trade, newStatus);

        store.saveToDisk();
        System.out.println("상태가 변경되었습니다.");
    }

    /**
     * 거래가 본인 거래인지 검증 (buyer 또는 seller 포함 여부 확인)
     */
    private boolean validateIsMyTradeOrWarn(User currentUser, Trade trade) {
        if (trade == null) {
            System.out.println("거래가 없습니다.");
            return false;
        }
        if (!currentUser.getId().equals(trade.getSellerUserId()) && !currentUser.getId().equals(trade.getBuyerUserId())) {
            System.out.println("본인 거래만 변경 가능");
            return false;
        }
        return true;
    }

    /**
     * 상태 변경 입력 메뉴 → 선택한 TradeStatus 반환
     */
    private TradeStatus readTradeStatus() {
        System.out.println("새 상태: 1.수락(ACCEPTED) 2.진행중(IN_PROGRESS) 3.완료(COMPLETED) 4.취소(CANCELLED)");
        int statusOption = InputUtil.readIntInRange("선택: ", 1, 4);
        return switch (statusOption) {
            case 1 -> TradeStatus.ACCEPTED;
            case 2 -> TradeStatus.IN_PROGRESS;
            case 3 -> TradeStatus.COMPLETED;
            case 4 -> TradeStatus.CANCELLED;
            default -> null;
        };
    }

    /**
     * Trade 상태 전환 수행 (Trade 내부 메서드 호출)
     */
    private void applyStatusChange(Trade trade, TradeStatus newStatus) {
        switch (newStatus) {
            case ACCEPTED:
                trade.acceptTrade();
                break;
            case IN_PROGRESS:
                trade.startTradeProgress();
                break;
            case COMPLETED:
                trade.completeTrade();
                break;
            case CANCELLED:
                trade.cancelTrade();
                break;
            default:
                break; // REQUESTED 등은 직접 전환하지 않음
        }
    }

    /**
     * syncRelatedPostStatus
     * - 거래 상태 변경 시 연관 게시글(Post)의 상태를 동기화
     * <p>
     * 규칙:
     * - ACCEPTED/IN_PROGRESS → PostStatus.IN_PROGRESS
     * - COMPLETED            → PostStatus.COMPLETED
     * - CANCELLED            → 완료 상태가 아니면 ON_SALE로 되돌림
     */
    private void syncRelatedPostStatus(Trade trade, TradeStatus newStatus) {
        int postId = trade.getRelatedPostId();
        Post post = store.posts().get(postId);
        if (post == null) return;

        switch (newStatus) {
            case ACCEPTED:
            case IN_PROGRESS:
                post.setStatus(PostStatus.IN_PROGRESS);
                break;
            case COMPLETED:
                post.setStatus(PostStatus.COMPLETED);
                break;
            case CANCELLED:
                if (post.getStatus() != PostStatus.COMPLETED) {
                    post.setStatus(PostStatus.ON_SALE);
                }
                break;
            default:
                break;
        }
    }

    /**
     * notifyCounterpartyOnStatus
     * - 상태 변경 시 상대방에게 알림 발송
     * <p>
     * 알림 규칙:
     * - COMPLETED → TRADE_COMPLETED
     * - 그 외 상태 → TRADE_ACCEPTED (추후 세분화 가능)
     */
    private void notifyCounterpartyOnStatus(User currentUser, Trade trade, TradeStatus newStatus) {
        String counterpartyUserId = currentUser.getId().equals(trade.getSellerUserId())
                ? trade.getBuyerUserId()
                : trade.getSellerUserId();

        NotificationType type = (newStatus == TradeStatus.COMPLETED)
                ? NotificationType.TRADE_COMPLETED
                : NotificationType.TRADE_ACCEPTED;

        int notificationId = store.nextNotificationId();
        Notification notification = new Notification(
                notificationId,
                counterpartyUserId,
                type,
                String.format("거래[%d] 상태가 %s로 변경되었습니다.",
                        trade.getTradeId(),
                        trade.getTradeStatus().getLabel())
        );
        store.notifications().put(notificationId, notification);
    }

    // ===================== 평가 플로우 =====================

    /**
     * handleEvaluationFlow
     * - 거래 신뢰도 평가 절차 (완료된 거래만 가능)
     * <p>
     * 처리 흐름:
     * 1) 거래 ID 입력 → 완료 상태 여부 + 본인 거래 여부 확인
     * 2) 상대방 사용자 ID 식별
     * 3) 평가 선택 (good/bad/취소)
     * 4) Trade 객체에 평가 기록
     * 5) 상대방 User의 신뢰도(Trust) 반영
     * 6) 저장
     */
    private void handleEvaluationFlow(User currentUser) {
        int tradeId = InputUtil.readInt("평가할 거래 ID: ");
        Trade trade = store.trades().get(tradeId);
        if (!validateIsCompletedMyTradeOrWarn(currentUser, trade)) return;

        String counterpartyUserId = resolveCounterpartyId(currentUser, trade);

        boolean isBuyer = currentUser.getId().equals(trade.getBuyerUserId());
        if (isBuyer && trade.getBuyerEvaluationGood() != null) {
            System.out.println("이미 평가를 완료하였습니다(구매자).");
            return;
        }
        if (!isBuyer && trade.getSellerEvaluationGood() != null) {
            System.out.println("이미 평가를 완료하였습니다(판매자).");
            return;
        }

        Boolean isGood = readGoodBadChoice(); // true=good, false=bad, null=취소
        if (isGood == null) {
            System.out.println("평가를 취소했습니다.");
            return;
        }

        if (isBuyer) trade.evaluateByBuyer(isGood);
        else trade.evaluateBySeller(isGood);

        applyTrustEvaluation(counterpartyUserId, isGood);

        store.saveToDisk();
        System.out.println("평가가 반영되었습니다.");
    }

    /**
     * 평가: 거래가 완료 상태인지 + 본인 거래인지 확인
     */
    private boolean validateIsCompletedMyTradeOrWarn(User currentUser, Trade trade) {
        if (trade == null || trade.getTradeStatus() != TradeStatus.COMPLETED) {
            System.out.println("완료된 거래만 평가 가능");
            return false;
        }
        if (!currentUser.getId().equals(trade.getSellerUserId()) && !currentUser.getId().equals(trade.getBuyerUserId())) {
            System.out.println("본인 거래만 평가 가능");
            return false;
        }
        return true;
    }

    /**
     * 현재 사용자와 반대쪽 사용자 ID 반환
     */
    private String resolveCounterpartyId(User currentUser, Trade trade) {
        return currentUser.getId().equals(trade.getBuyerUserId())
                ? trade.getSellerUserId()
                : trade.getBuyerUserId();
    }

    /**
     * 평가 입력 (1=good, 2=bad, 0=취소)
     */
    private Boolean readGoodBadChoice() {
        System.out.println("평가: 1.good  2.bad  0.취소");
        int choice = InputUtil.readIntInRange("선택: ", 0, 2);
        if (choice == 0) return null;
        return choice == 1;
    }

    /**
     * applyTrustEvaluation
     * - 신뢰도 평가 반영
     * · true(good)  → addTrustGood()
     * · false(bad) → addTrustBad()
     */
    private void applyTrustEvaluation(String targetUserId, boolean isGood) {
        User targetUser = store.users().get(targetUserId);
        if (targetUser == null) {
            System.out.println("대상 사용자가 존재하지 않습니다.");
            return;
        }
        if (isGood) targetUser.addTrustGood();
        else targetUser.addTrustBad();
    }
}