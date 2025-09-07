package org.example.service;

import org.example.datastore.DataStore;
import org.example.model.Notification;
import org.example.model.User;
import org.example.util.InputUtil;
import org.example.util.SortUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * NotificationService
 * -------------------
 * 사용자 알림(Notification) 조회 및 읽음 처리 기능을 제공하는 서비스.
 * <p>
 * 기능:
 *  - 현재 사용자(me)의 알림 목록 조회
 *  - 단일 알림 읽음 처리(사용자 소유 여부 확인 후)
 * <p>
 * 설계 노트:
 *  - 영속 계층(DataStore)의 notifications 맵을 조회하여 필터링/정렬한다.
 *  - 알림 목록이 비어 있으면 즉시 종료한다(불필요한 입력 방지).
 *  - 읽음 처리 시, 선택한 알림이 현재 사용자 소유인지 검증한다(권한 확인).
 */
public class NotificationService {
    /** 데이터 저장/로드 및 컬렉션 보관소 */
    private final DataStore store;

    /**
     * DataStore 주입 생성자
     * @param store 애플리케이션 전역 데이터 저장/로드를 담당하는 저장소
     */
    public NotificationService(DataStore store) {
        this.store = store;
    }

    /**
     * 내 알림함 표시 및 선택적 읽음 처리
     * <p>
     * 흐름:
     *  1) store.notifications()에서 현재 사용자(me)의 알림만 필터링
     *  2) 알림 ID(notificationId) 오름차순으로 정렬
     *  3) 콘솔에 목록 출력 (Notification.toString() 사용)
     *  4) 읽음 처리할 알림 번호 입력(0=스킵)
     *  5) 선택한 알림이 존재하고, 소유자 검사(me.getId() 일치) 통과 시 읽음 처리(markRead) 및 저장
     * <p>
     * 주의:
     *  - 읽음 처리 시 store.saveAll() 호출로 스냅샷 저장
     *  - 잘못된 선택(존재하지 않음/소유자 불일치) 시 안내 후 종료
     */
    public void showMyNotifications(User currentUser) {
        System.out.println("====== 알림함 ======");

        // 1) 현재 사용자 알림만 수집
        List<Notification> notificationList = new ArrayList<>();
        for (Notification notification : store.notifications().values()) {
            if (currentUser.getId().equals(notification.getRecipientUserId())) {
                notificationList.add(notification);
            }
        }

        // 2) 알림 ID 오름차순 정렬
        SortUtil.sortNotificationsById(notificationList);

        // 비어있으면 종료
        if (notificationList.isEmpty()) {
            System.out.println("알림이 없습니다.");
            return;
        }

        // 3) 출력
        for (Notification notification : notificationList) {
            System.out.println(notification); // [id] createdAt | type | message
        }

        // 4) 읽음 처리 대상 입력(0=스킵)
        System.out.println("읽음 처리할 알림 번호(0=스킵): ");
        int selectedNotificationId = InputUtil.readInt("선택: ");
        if (selectedNotificationId == 0) return;

        // 5) 대상 알림 조회 및 소유자 검증 후 읽음 처리
        Notification selectedNotification = store.notifications().get(selectedNotificationId);
        if (selectedNotification != null
                && currentUser.getId().equals(selectedNotification.getRecipientUserId())) {
            selectedNotification.markAsRead();
            store.saveToDisk();
            System.out.println("읽음 처리되었습니다.");
        } else {
            System.out.println("잘못된 선택입니다.");
        }
    }
}