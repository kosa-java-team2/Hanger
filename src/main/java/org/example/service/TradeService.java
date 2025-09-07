package org.example.service;

import org.example.datastore.DataStore;
import org.example.model.*;
import org.example.util.InputUtil;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
     *  <p>
     * @param buyer 거래를 요청하는 사용자(구매자)
     * @param post  대상 게시글
     */
    public void requestTrade(User buyer, Post post) {
        if (buyer.getId().equals(post.getSellerId())) {
            System.out.println("본인 게시글에는 거래 요청을 할 수 없습니다.");
            return;
        }
        int tid = store.nextTradeId();
        Trade t = new Trade(tid, post.getPostId(), buyer.getId(), post.getSellerId());
        store.trades().put(tid, t);

        // 판매자에게 거래 요청 알림
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

    // ===================== 거래 관리(진입점) =====================

    /**
     * 내 거래 관리 메뉴 진입점.
     *  - 내 거래 목록 출력
     *  - 1: 거래 상태 변경 / 2: 거래 평가 / 0: 뒤로
     */
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

    // ===================== 조회/렌더/입력 =====================

    /** 현재 사용자(me)가 buyer 또는 seller인 거래만 오름차순(ID) 정렬로 로드 */
    private List<Trade> loadUserTrades(User me) {
        return store.trades().values().stream()
                .filter(t -> me.getId().equals(t.getBuyerId()) || me.getId().equals(t.getSellerId()))
                .sorted(Comparator.comparingInt(Trade::getTradeId))
                .collect(Collectors.toList());
    }

    /** 거래 리스트 출력 + 메인 액션 메뉴 안내 */
    private void renderTrades(List<Trade> list) {
        for (Trade t : list) {
            System.out.printf("[%d] post=%d buyer=%s seller=%s status=%s (%s~%s)%n",
                    t.getTradeId(), t.getPostId(), t.getBuyerId(), t.getSellerId(), t.getStatus(),
                    t.getCreatedAt(), t.getUpdatedAt());
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

    /** 거래가 존재하고, 현재 사용자(me)의 거래인지 검사(아니면 경고 출력) */
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

    /** 상태 변경 입력 옵션 */
    private enum TradeStatusChoice {ACCEPT, IN_PROGRESS, COMPLETED, CANCELLED, INVALID}

    /** 상태 변경 메뉴 입력 → 열거형으로 반환 */
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

    /** 선택된 상태에 따라 Trade 상태 전이 수행 */
    private void applyStatusChange(Trade t, TradeStatusChoice choice) {
        switch (choice) {
            case ACCEPT -> t.markAccepted();
            case IN_PROGRESS -> t.markInProgress();
            case COMPLETED -> t.markCompleted();
            case CANCELLED -> t.markCancelled();
            default -> { /* no-op */ }
        }
    }

    /**
     * 상태 변경에 따른 상대방 알림 전송.
     * 단순화 맵핑:
     *  - COMPLETED → TRADE_COMPLETED
     *  - 그 외(ACCEPT/IN_PROGRESS/CANCELLED) → TRADE_ACCEPTED (※ 필요시 타입 세분화 권장)
     */
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

    // ===================== 평가 플로우 =====================

    /**
     * 신뢰도 평가 플로우(완료된 거래만 가능):
     *  1) 거래 ID 입력 → 완료 상태/본인 거래 검증
     *  2) 상대방 사용자 식별(buyer ↔ seller)
     *  3) good/bad 입력 → 상대방 User 신뢰도 반영
     *  4) (선택) 신고 접수 → Report 생성 + 관리자 알림
     *  5) 저장
     */
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

    /** 거래가 완료 상태인지, 그리고 현재 사용자 거래인지 검사 */
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

    /** 현재 사용자와 반대편 사용자 ID 반환 */
    private String resolveCounterpartyId(User me, Trade t) {
        return me.getId().equals(t.getBuyerId()) ? t.getSellerId() : t.getBuyerId();
    }

    /**
     * 신뢰도 평가 적용:
     *  - 1: good → addTrustGood()
     *  - 2: bad  → addTrustBad()
     */
    private void applyTrustEvaluation(String targetUserId) {
        User target = store.users().get(targetUserId);
        System.out.println("평가: 1.good  2.bad");
        int v = InputUtil.readIntInRange("선택: ", 1, 2);
        if (v == 1) target.addTrustGood();
        else target.addTrustBad();
    }

    /**
     * (선택) 신고 처리:
     *  - '예' 선택 시 Report 생성 → reports 맵에 저장
     *  - 관리자에게 REPORT_RECEIVED 알림 발송
     */
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