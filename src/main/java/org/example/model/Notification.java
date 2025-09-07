package org.example.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Notification implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int notificationId;
    private final String userId; // 수신자
    private final NotificationType type;
    private final String message;
    private boolean read;
    private final LocalDateTime createdAt = LocalDateTime.now();

    public Notification(int notificationId, String userId, NotificationType type, String message) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.type = type;
        this.message = message;
    }

    public int getNotificationId() { return notificationId; }
    public String getUserId() { return userId; }
    public NotificationType getType() { return type; }
    public String getMessage() { return message; }
    public boolean isRead() { return read; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void markRead() { this.read = true; }

    @Override
    public String toString() {
        return String.format("[%d] %s | %s | %s", notificationId, createdAt, type, message);
    }
}


