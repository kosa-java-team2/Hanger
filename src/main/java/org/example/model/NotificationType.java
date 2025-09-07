package org.example.model;

/**
 * NotificationType
 * -------------------
 * 알림(Notification)의 종류를 정의하는 열거형.
 * <p>
 * 발신(예시):
 * - TradeService.requestTrade(...)              → TRADE_REQUEST
 * - TradeService.notifyCounterpartyOnStatus(...)→ TRADE_ACCEPTED / TRADE_COMPLETED
 * - TradeService.maybeReportUser(...)           → REPORT_RECEIVED
 * <p>
 * 사용(예시):
 * - NotificationService.showMyNotifications(...) 에서 타입에 따라 메시지를 표시.
 * <p>
 * 호환성 노트:
 * - 본 프로젝트는 직렬화를 통해 {@link org.example.model.Notification} 이 저장됩니다.
 *   enum 상수명을 변경/삭제하면 이전에 저장된 데이터와 호환성이 깨질 수 있습니다.
 *   새로운 타입 추가 시에는(예: TRADE_IN_PROGRESS, TRADE_CANCELLED, SYSTEM 등) 기존 상수는 유지하세요.
 */
public enum NotificationType {

    /** 구매자가 거래를 요청했을 때(판매자에게 발송). */
    TRADE_REQUEST,

    /**
     * 거래 상태가 수락/진행/취소 등으로 변경되었음을 알릴 때 사용.
     * 현재 구현에서는 ACCEPTED/IN_PROGRESS/CANCELLED 상태 변경에 공용으로 사용됨.
     * (필요 시 TRADE_IN_PROGRESS, TRADE_CANCELLED 등으로 세분화 권장)
     */
    TRADE_ACCEPTED,

    /** 거래가 완료되었음을 상대방에게 알릴 때 사용. */
    TRADE_COMPLETED,

    /** 신고가 접수되었음을 관리자에게 알릴 때 사용. */
    REPORT_RECEIVED
}