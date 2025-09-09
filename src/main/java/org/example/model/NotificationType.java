package org.example.model;

import lombok.Getter;

/**
 * NotificationType
 * -------------------
 * 알림(Notification)의 종류를 정의하는 열거형(Enum).
 * <p>
 * 사용 목적:
 * - Notification 객체가 어떤 "이벤트/상황"에서 발생했는지를 구분하기 위함.
 * - 서비스 로직과 UI에서 알림 타입에 따라 다른 메시지, 아이콘, 행동을 지정할 수 있다.
 * <p>
 * 발신 예시:
 * - TradeService.requestTrade(...) 호출 시         → TRADE_REQUEST
 * - TradeService.notifyCounterpartyOnStatus(...)  → TRADE_ACCEPTED, TRADE_COMPLETED
 * <p>
 * 사용 예시:
 * - NotificationService.showMyNotifications(...) 에서
 * 타입별 라벨(label)을 이용해 "거래 요청", "거래 상태 변경", "거래 완료" 등의 메시지를 출력한다.
 * <p>
 * 호환성 주의:
 * - 본 프로젝트는 직렬화를 통해 {@link org.example.model.Notification} 이 저장되므로,
 * enum 상수명을 변경하거나 삭제하면 이전에 저장된 데이터와 호환되지 않는다.
 * - 새로운 타입 추가(예: TRADE_IN_PROGRESS, TRADE_CANCELLED, SYSTEM 등)는 가능하되,
 * 기존 상수는 반드시 유지해야 한다.
 */
@Getter
public enum NotificationType {

    /**
     * 거래 요청 알림
     * - 구매자가 판매자에게 거래를 요청했을 때 발생
     * - 예: "홍길동님이 거래 요청을 보냈습니다."
     */
    TRADE_REQUEST("거래 요청"),

    /**
     * 거래 상태 변경 알림
     * - 거래가 수락, 진행, 취소 등으로 변경되었음을 알릴 때 사용
     * - 현재 구현에서는 ACCEPTED/IN_PROGRESS/CANCELLED 상태를 공용으로 처리한다.
     * - 필요하다면 TRADE_IN_PROGRESS, TRADE_CANCELLED 등 세부 타입으로 확장 가능
     * - 예: "거래가 수락되었습니다.", "거래가 취소되었습니다."
     */
    TRADE_ACCEPTED("거래 상태 변경"),

    /**
     * 거래 완료 알림
     * - 거래가 성공적으로 끝났음을 상대방에게 알림
     * - 구매자/판매자 모두에게 전달 가능
     * - 예: "거래가 완료되었습니다."
     */
    TRADE_COMPLETED("거래 완료");

    // ===================== 필드 =====================
    /**
     * 알림 유형의 한글 라벨
     * - UI 표시용 문자열
     * - 예: "거래 요청", "거래 상태 변경", "거래 완료"
     */
    private final String label;

    // ===================== 생성자 =====================

    /**
     * NotificationType 생성자
     *
     * @param label 알림 유형에 해당하는 한글 라벨
     */
    NotificationType(String label) {
        this.label = label;
    }
}