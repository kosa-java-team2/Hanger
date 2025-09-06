package org.example.service;

import java.util.List;
import java.util.stream.Collectors;

public class TradeService {
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

        // 알림 발송 (판매자에게 거래 요청)
        int nid = store.nextNotificationId();
        Notification n = new Notification(nid, post.getSellerId(), NotificationType.TRADE_REQUEST,
                String.format("%s 님이 [%d] %s 거래를 요청했습니다.", buyer.getNickname(), post.getPostId(), post.getTitle()));
        store.notifications().put(nid, n);

        store.saveAll();
        System.out.println("거래 요청이 전송되었습니다.");
    }

    // 내 거래 보기/상태 변경
    public void manageTrades(User me) {
        System.out.println("====== 내 거래 ======");
        List<Trade> list = store.trades().values().stream()
                .filter(t -> me.getId().equals(t.getBuyerId()) || me.getId().equals(t.getSellerId()))
                .sorted((a,b) -> Integer.compare(a.getTradeId(), b.getTradeId()))
                .collect(Collectors.toList());
        if (list.isEmpty()) {
            System.out.println("거래 내역이 없습니다.");
            return;
        }
        for (Trade t : list) {
            System.out.println(String.format("[%d] post=%d buyer=%s seller=%s status=%s (%s~%s)",
                    t.getTradeId(), t.getPostId(), t.getBuyerId(), t.getSellerId(), t.getStatus(),
                    t.getCreatedAt(), t.getUpdatedAt()));
        }
        System.out.println("1. 거래 상태 변경  2. 거래 평가(신뢰도)  0. 뒤로");
        int sel = InputUtil.readIntInRange("선택: ", 0, 2);
        if (sel == 0) return;
        if (sel == 1) {
            int tid = InputUtil.readInt("변경할 거래 ID: ");
            Trade t = store.trades().get(tid);
            if (t == null) { System.out.println("거래가 없습니다."); return; }
            if (!me.getId().equals(t.getSellerId()) && !me.getId().equals(t.getBuyerId())) { System.out.println("본인 거래만 변경 가능"); return; }
            System.out.println("새 상태: 1.수락 2.진행중 3.완료 4.취소");
            int st = InputUtil.readIntInRange("선택: ", 1, 4);
            switch (st) {
                case 1: t.markAccepted(); break;
                case 2: t.markInProgress(); break;
                case 3: t.markCompleted(); break;
                case 4: t.markCancelled(); break;
            }
            // 상태 변경에 따른 알림 예시
            String target = me.getId().equals(t.getSellerId()) ? t.getBuyerId() : t.getSellerId();
            NotificationType type = (st == 3) ? NotificationType.TRADE_COMPLETED : NotificationType.TRADE_ACCEPTED;
            int nid = store.nextNotificationId();
            Notification n = new Notification(nid, target, type,
                    String.format("거래[%d] 상태가 %s로 변경되었습니다.", t.getTradeId(), t.getStatus()));
            store.notifications().put(nid, n);
            store.saveAll();
            System.out.println("상태가 변경되었습니다.");
        } else if (sel == 2) {
            int tid = InputUtil.readInt("평가할 거래 ID: ");
            Trade t = store.trades().get(tid);
            if (t == null || t.getStatus() != TradeStatus.COMPLETED) { System.out.println("완료된 거래만 평가 가능"); return; }
            boolean meIsBuyer = me.getId().equals(t.getBuyerId());
            String targetUserId = meIsBuyer ? t.getSellerId() : t.getBuyerId();
            User target = store.users().get(targetUserId);
            System.out.println("평가: 1.good  2.bad");
            int v = InputUtil.readIntInRange("선택: ", 1, 2);
            boolean good = (v == 1);
            if (good) target.addTrustGood(); else target.addTrustBad();
            // 신고 여부
            System.out.println("신고하시겠습니까? (1=예, 2=아니오)");
            int r = InputUtil.readIntInRange("선택: ", 1, 2);
            if (r == 1) {
                int rid = store.nextReportId();
                String reason = InputUtil.readNonEmptyLine("신고 사유: ");
                Report rep = new Report(rid, me.getId(), targetUserId, reason);
                store.reports().put(rid, rep);
                int nid = store.nextNotificationId();
                store.notifications().put(nid, new Notification(nid, "admin", NotificationType.REPORT_RECEIVED,
                        String.format("신고 접수: %s -> %s (%s)", me.getId(), targetUserId, reason)));
            }
            store.saveAll();
            System.out.println("평가가 반영되었습니다.");
        }
    }
}


