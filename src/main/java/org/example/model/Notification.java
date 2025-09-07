package org.example.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Notification 클래스
 * -------------------
 * 시스템 내에서 특정 사용자에게 전달되는 알림(Notice)을 표현하는 모델 클래스.
 *
 * 특징:
 * - 직렬화 가능 (데이터 저장/로드 시 필요)
 * - 알림 ID, 수신자, 알림 유형, 메시지, 생성일시 등을 보관
 * - '읽음 여부(read)' 상태를 관리
 */
public class Notification implements Serializable {
    private static final long serialVersionUID = 1L;

    // ===================== 필드 =====================

    /** 알림 고유 ID (DataStore에서 시퀀스를 통해 발급) */
    private final int notificationId;

    /** 알림을 수신하는 사용자 ID */
    private final String userId;

    /** 알림 유형 (예: 거래 알림, 신고 알림 등) */
    private final NotificationType type;

    /** 알림에 표시될 메시지 */
    private final String message;

    /** 사용자가 읽었는지 여부 (true=읽음, false=안 읽음) */
    private boolean read;

    /** 알림 생성 시각 (기본적으로 객체 생성 시점으로 고정) */
    private final LocalDateTime createdAt = LocalDateTime.now();

    // ===================== 생성자 =====================
    /**
     * Notification 객체 생성자
     *
     * @param notificationId 알림 ID
     * @param userId         알림 수신자 ID
     * @param type           알림 유형 (enum)
     * @param message        알림 메시지 내용
     */
    public Notification(int notificationId, String userId, NotificationType type, String message) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.type = type;
        this.message = message;
    }

    // ===================== Getter 메서드 =====================
    public int getNotificationId() { return notificationId; }
    public String getUserId() { return userId; }
    public NotificationType getType() { return type; }
    public String getMessage() { return message; }
    public boolean isRead() { return read; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // ===================== 상태 변경 메서드 =====================
    /**
     * 알림을 '읽음' 상태로 변경한다.
     * (사용자가 알림을 확인했을 때 호출)
     */
    public void markRead() { this.read = true; }

    // ===================== toString =====================
    /**
     * 알림 정보를 사람이 읽기 쉬운 문자열로 반환
     * 형식: [알림ID] 생성시각 | 알림유형 | 메시지
     */
    @Override
    public String toString() {
        return String.format("[%d] %s | %s | %s", notificationId, createdAt, type, message);
    }
}