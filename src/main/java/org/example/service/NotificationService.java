package org.example.service;

import org.example.datastore.DataStore;
import org.example.model.Notification;
import org.example.model.User;
import org.example.util.InputUtil;

import java.util.List;
import java.util.stream.Collectors;

public class NotificationService {
    private final DataStore store;

    public NotificationService(DataStore store) {
        this.store = store;
    }


    public void showMyNotifications(User me) {
        System.out.println("====== 알림함 ======");
        List<Notification> list = store.notifications().values().stream()
                .filter(n -> n.getUserId().equals(me.getId()))
                .sorted((a,b) -> Integer.compare(a.getNotificationId(), b.getNotificationId()))
                .collect(Collectors.toList());
        if (list.isEmpty()) {
            System.out.println("알림이 없습니다.");
            return;
        }
        for (Notification n : list) {
            System.out.println(n);
        }
        System.out.println("읽음 처리할 알림 번호(0=스킵): ");
        int nid = InputUtil.readInt("선택: ");
        if (nid == 0) return;
        Notification n = store.notifications().get(nid);
        if (n != null && me.getId().equals(n.getUserId())) {
            n.markRead();
            store.saveAll();
            System.out.println("읽음 처리되었습니다.");
        } else {
            System.out.println("잘못된 선택입니다.");
        }
    }
}


