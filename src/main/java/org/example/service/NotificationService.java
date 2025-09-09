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
 * 사용자 알림(Notification) 조회 및 삭제 기능을 제공하는 서비스 레이어.
 * <p>
 * 기능:
 * - 현재 사용자(me)의 알림 목록 조회
 * - 단일 알림 삭제 (사용자 소유 여부 확인 필수)
 * <p>
 * 설계 노트:
 * - 알림은 DataStore.notifications(Map<Integer, Notification>)에 저장된다.
 * - 알림 조회 시 현재 로그인 사용자 ID(recipientUserId 일치)로 필터링한다.
 * - 알림은 기본적으로 ID 오름차순으로 정렬해 출력한다.
 * - 삭제는 물리적 삭제(remove)이며, 소유자 검증을 반드시 수행한다.
 * - 삭제 후에는 DataStore.saveToDisk() 호출로 변경사항을 영구 저장한다.
 *
 * @param store 데이터 저장/로드 및 컬렉션을 관리하는 전역 저장소
 */
public record NotificationService(DataStore store) {
    /**
     * DataStore 주입 생성자
     *
     * @param store 애플리케이션 전역 데이터 저장/로드를 담당하는 저장소
     */
    public NotificationService {
    }

    // ===================== 알림 조회/삭제 =====================

    /**
     * 내 알림함 표시 및 선택적 삭제
     * <p>
     * 동작 흐름:
     * 1) store.notifications()에서 현재 사용자(me)의 알림만 필터링
     * 2) 알림 ID(notificationId) 기준 오름차순 정렬
     * 3) 콘솔에 알림 목록 출력 (Notification.toString() 사용)
     * 4) 삭제할 알림 번호 입력 (0 입력 시 스킵)
     * 5) 대상 알림 존재 여부 & 소유자 일치 여부 검사
     * 6) 조건 만족 시 store.notifications()에서 remove → saveToDisk()
     * <p>
     * 주의:
     * - 알림이 하나도 없으면 즉시 종료 (불필요한 입력 방지)
     * - 잘못된 선택(존재하지 않음, 소유자 불일치)은 안내 후 종료
     *
     * @param currentUser 현재 로그인한 사용자
     */
    public void showMyNotifications(User currentUser) {
        System.out.println("====== 알림함 ======");

        // 1) 현재 사용자(me)의 알림만 수집
        List<Notification> notificationList = new ArrayList<>();
        for (Notification notification : store.notifications().values()) {
            if (currentUser.getId().equals(notification.getRecipientUserId())) {
                notificationList.add(notification);
            }
        }

        // 2) 알림 ID 오름차순 정렬
        SortUtil.sortNotificationsById(notificationList);

        // 알림이 없으면 종료
        if (notificationList.isEmpty()) {
            System.out.println("알림이 없습니다.");
            return;
        }

        // 3) 알림 출력
        for (Notification notification : notificationList) {
            System.out.println(notification);
            // Notification.toString() → [id] createdAt | type | message
        }

        // 4) 삭제할 알림 ID 입력 (0 = 스킵)
        System.out.println("삭제할 알림 번호(0=스킵): ");
        int selectedNotificationId = InputUtil.readInt("선택: ");
        if (selectedNotificationId == 0) return;

        // 5) 대상 알림 조회 및 소유자 확인
        Notification selectedNotification = store.notifications().get(selectedNotificationId);
        if (selectedNotification != null
                && currentUser.getId().equals(selectedNotification.getRecipientUserId())) {
            // 6) 알림 삭제
            store.notifications().remove(selectedNotificationId);

            // 저장
            store.saveToDisk();
            System.out.println("알림이 삭제되었습니다.");
        } else {
            System.out.println("잘못된 선택입니다.");
        }
    }
}