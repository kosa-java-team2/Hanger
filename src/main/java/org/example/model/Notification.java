package org.example.model;

import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Notification 클래스
 * -------------------
 * 시스템 내에서 특정 사용자(User)에게 전달되는 알림(Notice)을 표현하는 모델 클래스.
 * <p>
 * 활용 예시:
 * - 거래가 성사되었을 때 상대방에게 "거래가 완료되었습니다" 알림 전달
 * - 게시글에 댓글/메시지가 달렸을 때 작성자에게 알림 전달
 * - 시스템 공지, 경고 메시지 등 다양한 유형의 알림 제공
 * <p>
 * 특징:
 * - 직렬화 가능 (Serializable): DataStore 저장/로드 시 Snapshot에 포함되기 위함
 * - 각 알림은 고유 ID(notificationId)를 가진다.
 * - 알림의 상태(읽음/안 읽음)를 isRead 필드로 관리한다.
 * - 생성 시각(createdAt)은 알림 생성 시 자동으로 기록된다.
 */
@Getter
public class Notification implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L; // 직렬화 버전 관리용 UID

    // ===================== 필드 =====================

    /**
     * 알림 고유 ID
     * - DataStore의 시퀀스(nextNotificationId)로 발급된다.
     * - 시스템 내에서 알림을 구분하는 기본 키 역할.
     */
    private final int notificationId;

    /**
     * 알림을 받는 사용자(User)의 ID
     * - User.username 같은 고유 식별자를 참조한다.
     * - 알림 대상자를 특정하기 위해 사용.
     */
    private final String recipientUserId;

    /**
     * 알림 유형 (NotificationType enum)
     * - 예: 거래 알림(TRADE), 시스템 공지(SYSTEM), 메시지 알림(MESSAGE) 등
     * - NotificationType은 label을 통해 사람이 읽기 좋은 이름을 제공한다.
     */
    private final NotificationType notificationType;

    /**
     * 알림에 표시될 메시지 내용
     * - 사용자가 알림 목록을 확인할 때 보여질 텍스트
     * - 예: "홍길동님이 거래 요청을 보냈습니다."
     */
    private final String notificationMessage;

    /**
     * 알림 읽음 상태
     * - true : 사용자가 알림을 확인함
     * - false : 아직 읽지 않음 (기본 상태)
     * - UI/UX에서 "새 알림" 뱃지를 띄우는 용도로 활용 가능
     */
    private boolean isRead;

    /**
     * 알림 생성 시각
     * - 객체 생성 시 LocalDateTime.now()로 자동 기록
     * - 알림 시간순 정렬, "5분 전" 표시 등에 활용 가능
     */
    private final LocalDateTime createdAt = LocalDateTime.now();

    // ===================== 생성자 =====================

    /**
     * Notification 객체 생성자
     *
     * @param notificationId      알림 고유 ID (DataStore에서 발급)
     * @param recipientUserId     알림 수신자 ID
     * @param notificationType    알림 유형 (NotificationType enum)
     * @param notificationMessage 알림 메시지 본문
     *                            <p>
     *                            생성자 호출 시:
     *                            - isRead는 기본값 false
     *                            - createdAt은 현재 시간으로 자동 설정됨
     */
    public Notification(int notificationId, String recipientUserId, NotificationType notificationType, String notificationMessage) {
        this.notificationId = notificationId;
        this.recipientUserId = recipientUserId;
        this.notificationType = notificationType;
        this.notificationMessage = notificationMessage;
    }

    // ===================== toString =====================

    /**
     * 알림 객체를 사람이 읽기 쉬운 문자열로 반환한다.
     * <p>
     * 출력 형식:
     * [알림ID] 생성시각 | 알림유형 | 메시지
     * <p>
     * 예:
     * [3001] 2025-09-09T13:25:00 | 거래 알림 | "거래가 완료되었습니다."
     */
    @Override
    public String toString() {
        return String.format("[%d] %s | %s | %s",
                notificationId,
                createdAt,
                notificationType.getLabel(), // enum의 label을 사용해 가독성 높임
                notificationMessage);
    }
}