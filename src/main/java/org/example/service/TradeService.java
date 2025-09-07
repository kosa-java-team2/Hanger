package org.example.service;

import org.example.datastore.DataStore;
import org.example.model.*;
import org.example.util.InputUtil;
import org.example.util.SortUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * TradeService
 * -------------------
 * 거래(Trade) 수명주기 전반을 다루는 서비스 레이어.
 * <p>
 * 제공 기능:
 *  - requestTrade: 게시글 상세에서 거래 요청 생성 + 판매자 알림 발송
 *  - manageTrades: 내 거래 목록 조회 → 상태 변경 / 신뢰도 평가(신고 포함)
 * <p>
 * 설계 노트:
 *  - 모든 영속 데이터는 DataStore를 통해 접근/수정/저장한다.
 *  - 상태 변경/평가 후에는 store.saveAll()로 스냅샷 저장(지속성 보장).
 *  - 알림(Notification)은 counterparty(상대방)에게 전송한다.
 *  - 신고 시, 관리자(ADMIN_ID)에게 접수 알림을 보낸다.
 */
public class TradeService {
    /** 신고 접수 알림을 전달할 관리자 계정 ID (공용 상수 클래스로 이동 권장) */
    private static final String ADMIN_ID = "admin";
    private final DataStore store;

    public TradeService(DataStore store) {
        this.store = store;
    }

    // ===================== 거래 요청 =====================

    /**
     * 게시글 상세 화면에서 구매자가 거래 요청을 수행.
     * 흐름:
     *  1) 본인 글 요청 방지(구매자=판매자면 중단)
     *  2) tradeId 발급 → Trade 생성 → trades 맵에 저장
     *  3) 판매자에게 거래 요청 알림(NotificationType.TRADE_REQUEST) 발송
     *  4) store.saveAll() 호출
     *
     * @param buyer 거래를 요청하는 사용자(구매자)
     * @param post  대상 게시글
     */
    public void requestTrade(User buyer, Post post) {
        if (buyer.getId().equals(post.getSellerId())) {
            System.out.println("본인 게시글에는 거래 요청을 할 수 없습니다.");
            return;
        }
        int tradeId = store.nextTradeId();
        Trade trade = new Trade(tradeId, post.getPostId(), buyer.getId(), post.getSellerId());
        store.trades().put(tradeId, trade);

        // 판매자에게 거래 요청 알림
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
     * 내 거래 관리 메뉴 진입점.
     *  - 내 거래 목록 출력
     *  - 1: 거래 상태 변경 / 2: 거래 평가 / 0: 뒤로
     */
    public void manageTrades(User currentUser) {
        System.out.println("====== 내 거래 ======");

        // 재사용: loadUserTrades 호출
        List<Trade> myTrades = loadUserTrades(currentUser);

        if (myTrades.isEmpty()) {
            System.out.println("거래 내역이 없습니다.");
            return;
        }
        renderTrades(myTrades);

        switch (readMainAction()) {
            case 0 -> System.out.println("뒤로 이동합니다.");
            case 1 -> handleStatusChangeFlow(currentUser);
            case 2 -> handleEvaluationFlow(currentUser);
            default -> System.out.println("잘못된 선택입니다.");
        }
    }

    // ===================== 조회/렌더/입력 =====================

    /** 현재 사용자(currentUser)가 buyer 또는 seller인 거래만 오름차순(ID) 정렬로 로드 */
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

    /** 거래 리스트 출력 + 메인 액션 메뉴 안내 */
    private void renderTrades(List<Trade> trades) {
        for (Trade trade : trades) {
            System.out.printf("[%d] post=%d buyer=%s seller=%s status=%s (%s~%s)%n",
                    trade.getTradeId(), trade.getRelatedPostId(), trade.getBuyerUserId(), trade.getSellerUserId(),
                    trade.getTradeStatus(), trade.getCreatedAt(), trade.getUpdatedAt());
        }
        System.out.println("1. 거래 상태 변경  2. 거래 평가(신뢰도)  0. 뒤로");
    }

    /** 메인 메뉴 선택 입력(0~2) */
    private int readMainAction() {
        return InputUtil.readIntInRange("선택: ", 0, 2);
    }

    // ===================== 상태 변경 플로우 =====================

    /**
     * 상태 변경 플로우:
     *  1) 거래 ID 입력 → 본인 거래인지 검증
     *  2) 상태 선택 입력(수락/진행중/완료/취소)
     *  3) Trade 상태 적용
     *  4) 상대방에게 상태 변경 알림 발송
     *  5) 저장
     */
    private void handleStatusChangeFlow(User currentUser) {
        int tradeId = InputUtil.readInt("변경할 거래 ID: ");
        Trade trade = store.trades().get(tradeId);
        if (!validateIsMyTradeOrWarn(currentUser, trade)) return;

        TradeStatusChoice statusChoice = readTradeStatusChoice();
        if (statusChoice == TradeStatusChoice.INVALID) {
            System.out.println("잘못된 선택");
            return;
        }

        applyStatusChange(trade, statusChoice);
        notifyCounterpartyOnStatus(currentUser, trade, statusChoice);
        store.saveToDisk();
        System.out.println("상태가 변경되었습니다.");
    }

    /** 거래가 존재하고, 현재 사용자(currentUser)의 거래인지 검사(아니면 경고 출력) */
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

    /** 상태 변경 입력 옵션 */
    private enum TradeStatusChoice {ACCEPT, IN_PROGRESS, COMPLETED, CANCELLED, INVALID}

    /** 상태 변경 메뉴 입력 → 열거형으로 반환 */
    private TradeStatusChoice readTradeStatusChoice() {
        System.out.println("새 상태: 1.수락 2.진행중 3.완료 4.취소");
        int statusOption = InputUtil.readIntInRange("선택: ", 1, 4);
        return switch (statusOption) {
            case 1 -> TradeStatusChoice.ACCEPT;
            case 2 -> TradeStatusChoice.IN_PROGRESS;
            case 3 -> TradeStatusChoice.COMPLETED;
            case 4 -> TradeStatusChoice.CANCELLED;
            default -> TradeStatusChoice.INVALID;
        };
    }

    /** 선택된 상태에 따라 Trade 상태 전이 수행 */
    private void applyStatusChange(Trade trade, TradeStatusChoice choice) {
        switch (choice) {
            case ACCEPT -> trade.acceptTrade();
            case IN_PROGRESS -> trade.startTradeProgress();
            case COMPLETED -> trade.completeTrade();
            case CANCELLED -> trade.cancelTrade();
            default -> { }
        }
    }

    /**
     * 상태 변경에 따른 상대방 알림 전송.
     * 단순화 맵핑:
     *  - COMPLETED → TRADE_COMPLETED
     *  - 그 외(ACCEPT/IN_PROGRESS/CANCELLED) → TRADE_ACCEPTED (※ 필요시 타입 세분화 권장)
     */
    private void notifyCounterpartyOnStatus(User currentUser, Trade trade, TradeStatusChoice choice) {
        String counterpartyUserId = currentUser.getId().equals(trade.getSellerUserId())
                ? trade.getBuyerUserId()
                : trade.getSellerUserId();
        NotificationType notificationType = (choice == TradeStatusChoice.COMPLETED)
                ? NotificationType.TRADE_COMPLETED
                : NotificationType.TRADE_ACCEPTED;

        int notificationId = store.nextNotificationId();
        Notification notification = new Notification(
                notificationId,
                counterpartyUserId,
                notificationType,
                String.format("거래[%d] 상태가 %s로 변경되었습니다.", trade.getTradeId(), trade.getTradeStatus())
        );
        store.notifications().put(notificationId, notification);
    }

    // ===================== 평가 플로우 =====================

    /**
     * 신뢰도 평가 플로우(완료된 거래만 가능):
     *  1) 거래 ID 입력 → 완료 상태/본인 거래 검증
     *  2) 상대방 사용자 식별(buyer ↔ seller)
     *  3) good/bad 입력 → 상대방 User 신뢰도 반영
     *  4) (선택) 신고 접수 → Report 생성 + 관리자 알림
     *  5) 저장
     */
    private void handleEvaluationFlow(User currentUser) {
        int tradeId = InputUtil.readInt("평가할 거래 ID: ");
        Trade trade = store.trades().get(tradeId);
        if (!validateIsCompletedMyTradeOrWarn(currentUser, trade)) return;

        String counterpartyUserId = resolveCounterpartyId(currentUser, trade);
        applyTrustEvaluation(counterpartyUserId);
        maybeReportUser(currentUser.getId(), counterpartyUserId);

        store.saveToDisk();
        System.out.println("평가가 반영되었습니다.");
    }

    /** 거래가 완료 상태인지, 그리고 현재 사용자 거래인지 검사 */
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

    /** 현재 사용자와 반대편 사용자 ID 반환 */
    private String resolveCounterpartyId(User currentUser, Trade trade) {
        return currentUser.getId().equals(trade.getBuyerUserId())
                ? trade.getSellerUserId()
                : trade.getBuyerUserId();
    }

    /**
     * 신뢰도 평가 적용:
     *  - 1: good → addTrustGood()
     *  - 2: bad  → addTrustBad()
     */
    private void applyTrustEvaluation(String targetUserId) {
        User targetUser = store.users().get(targetUserId);
        System.out.println("평가: 1.good  2.bad");
        int choice = InputUtil.readIntInRange("선택: ", 1, 2);
        if (choice == 1) targetUser.addTrustGood();
        else targetUser.addTrustBad();
    }

    /**
     * (선택) 신고 처리:
     *  - '예' 선택 시 Report 생성 → reports 맵에 저장
     *  - 관리자에게 REPORT_RECEIVED 알림 발송
     */
    private void maybeReportUser(String reporterUserId, String reportedTargetUserId) {
        System.out.println("신고하시겠습니까? (1=예, 2=아니오)");
        int confirmation = InputUtil.readIntInRange("선택: ", 1, 2);
        if (confirmation != 1) return;

        int reportId = store.nextReportId();
        String reportReason = InputUtil.readNonEmptyLine("신고 사유: ");
        Report report = new Report(reportId, reporterUserId, reportedTargetUserId, reportReason);
        store.reports().put(reportId, report);

        int notificationId = store.nextNotificationId();
        Notification notification = new Notification(
                notificationId,
                ADMIN_ID,
                NotificationType.REPORT_RECEIVED,
                String.format("신고 접수: %s -> %s (%s)", reporterUserId, reportedTargetUserId, reportReason)
        );
        store.notifications().put(notificationId, notification);
    }
}