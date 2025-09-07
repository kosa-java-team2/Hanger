package org.example.service;

import org.example.datastore.DataStore;
import org.example.model.*;
import org.example.util.InputUtil;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TradeService {
    private static final String ADMIN_ID = "admin"; // 필요시 공용 Constants로 이동 권장
    private final DataStore store;

    public TradeService(DataStore store) {
        this.store = store;
    }

    // 게시글 상세에서 거래 요청
    public void requestTrade(User buyer, Post post) {
        if (buyer.getId().equals(post.getSellerId())) {
            System.out.println("본인 게시글에는 거래 요청을 할 수 없습니다.");
            return;
        }
        int tid = store.nextTradeId();
        Trade t = new Trade(tid, post.getPostId(), buyer.getId(), post.getSellerId());
        store.trades().put(tid, t);

        // 알림(판매자에게 거래 요청)
        int nid = store.nextNotificationId();
        Notification n = new Notification(
                nid,
                post.getSellerId(),
                NotificationType.TRADE_REQUEST,
                String.format("%s 님이 [%d] %s 거래를 요청했습니다.", buyer.getNickname(), post.getPostId(), post.getTitle())
        );
        store.notifications().put(nid, n);

        store.saveAll();
        System.out.println("거래 요청이 전송되었습니다.");
    }

    // ===================== 리팩터링된 관리 진입점 =====================
    public void manageTrades(User me) {
        System.out.println("====== 내 거래 ======");
        List<Trade> list = loadUserTrades(me);
        if (list.isEmpty()) {
            System.out.println("거래 내역이 없습니다.");
            return;
        }
        renderTrades(list);

        switch (readMainAction()) {
            case 0 -> {
                System.out.println("뒤로 이동합니다.");
            }
            case 1 -> handleStatusChangeFlow(me);
            case 2 -> handleEvaluationFlow(me);
            default -> System.out.println("잘못된 선택입니다.");
        }
    }
    // ===============================================================

    // --- 조회/렌더링/입력 ---
    private List<Trade> loadUserTrades(User me) {
        return store.trades().values().stream()
                .filter(t -> me.getId().equals(t.getBuyerId()) || me.getId().equals(t.getSellerId()))
                .sorted(Comparator.comparingInt(Trade::getTradeId))
                .collect(Collectors.toList());
    }

    private void renderTrades(List<Trade> list) {
        for (Trade t : list) {
            System.out.printf("[%d] post=%d buyer=%s seller=%s status=%s (%s~%s)%n",
                    t.getTradeId(), t.getPostId(), t.getBuyerId(), t.getSellerId(), t.getStatus(),
                    t.getCreatedAt(), t.getUpdatedAt());
        }
        System.out.println("1. 거래 상태 변경  2. 거래 평가(신뢰도)  0. 뒤로");
    }

    private int readMainAction() {
        return InputUtil.readIntInRange("선택: ", 0, 2);
    }

    // --- 상태 변경 플로우 ---
    private void handleStatusChangeFlow(User me) {
        int tid = InputUtil.readInt("변경할 거래 ID: ");
        Trade t = store.trades().get(tid);
        if (!isMyTradeOrWarn(me, t)) return;

        TradeStatusChoice choice = readTradeStatusChoice();
        if (choice == TradeStatusChoice.INVALID) {
            System.out.println("잘못된 선택");
            return;
        }

        applyStatusChange(t, choice);
        notifyCounterpartyOnStatus(me, t, choice);
        store.saveAll();
        System.out.println("상태가 변경되었습니다.");
    }

    private boolean isMyTradeOrWarn(User me, Trade t) {
        if (t == null) {
            System.out.println("거래가 없습니다.");
            return false;
        }
        if (!me.getId().equals(t.getSellerId()) && !me.getId().equals(t.getBuyerId())) {
            System.out.println("본인 거래만 변경 가능");
            return false;
        }
        return true;
    }

    private enum TradeStatusChoice {ACCEPT, IN_PROGRESS, COMPLETED, CANCELLED, INVALID}

    private TradeStatusChoice readTradeStatusChoice() {
        System.out.println("새 상태: 1.수락 2.진행중 3.완료 4.취소");
        int st = InputUtil.readIntInRange("선택: ", 1, 4);
        return switch (st) {
            case 1 -> TradeStatusChoice.ACCEPT;
            case 2 -> TradeStatusChoice.IN_PROGRESS;
            case 3 -> TradeStatusChoice.COMPLETED;
            case 4 -> TradeStatusChoice.CANCELLED;
            default -> TradeStatusChoice.INVALID;
        };
    }

    private void applyStatusChange(Trade t, TradeStatusChoice choice) {
        switch (choice) {
            case ACCEPT -> t.markAccepted();
            case IN_PROGRESS -> t.markInProgress();
            case COMPLETED -> t.markCompleted();
            case CANCELLED -> t.markCancelled();
            default -> { /* no-op */ }
        }
    }

    private void notifyCounterpartyOnStatus(User me, Trade t, TradeStatusChoice choice) {
        String target = me.getId().equals(t.getSellerId()) ? t.getBuyerId() : t.getSellerId();
        NotificationType type = (choice == TradeStatusChoice.COMPLETED)
                ? NotificationType.TRADE_COMPLETED
                : NotificationType.TRADE_ACCEPTED;

        int nid = store.nextNotificationId();
        Notification n = new Notification(
                nid,
                target,
                type,
                String.format("거래[%d] 상태가 %s로 변경되었습니다.", t.getTradeId(), t.getStatus())
        );
        store.notifications().put(nid, n);
    }

    // --- 평가 플로우 ---
    private void handleEvaluationFlow(User me) {
        int tid = InputUtil.readInt("평가할 거래 ID: ");
        Trade t = store.trades().get(tid);
        if (!isCompletedMyTradeOrWarn(me, t)) return;

        String targetUserId = resolveCounterpartyId(me, t);
        applyTrustEvaluation(targetUserId);
        maybeReportUser(me.getId(), targetUserId);

        store.saveAll();
        System.out.println("평가가 반영되었습니다.");
    }

    private boolean isCompletedMyTradeOrWarn(User me, Trade t) {
        if (t == null || t.getStatus() != TradeStatus.COMPLETED) {
            System.out.println("완료된 거래만 평가 가능");
            return false;
        }
        if (!me.getId().equals(t.getSellerId()) && !me.getId().equals(t.getBuyerId())) {
            System.out.println("본인 거래만 평가 가능");
            return false;
        }
        return true;
    }

    private String resolveCounterpartyId(User me, Trade t) {
        return me.getId().equals(t.getBuyerId()) ? t.getSellerId() : t.getBuyerId();
    }

    private void applyTrustEvaluation(String targetUserId) {
        User target = store.users().get(targetUserId);
        System.out.println("평가: 1.good  2.bad");
        int v = InputUtil.readIntInRange("선택: ", 1, 2);
        if (v == 1) target.addTrustGood();
        else target.addTrustBad();
    }

    private void maybeReportUser(String reporterId, String targetUserId) {
        System.out.println("신고하시겠습니까? (1=예, 2=아니오)");
        int r = InputUtil.readIntInRange("선택: ", 1, 2);
        if (r != 1) return;

        int rid = store.nextReportId();
        String reason = InputUtil.readNonEmptyLine("신고 사유: ");
        Report rep = new Report(rid, reporterId, targetUserId, reason);
        store.reports().put(rid, rep);

        int nid = store.nextNotificationId();
        store.notifications().put(
                nid,
                new Notification(nid, ADMIN_ID, NotificationType.REPORT_RECEIVED,
                        String.format("신고 접수: %s -> %s (%s)", reporterId, targetUserId, reason))
        );
    }
}