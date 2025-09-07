package org.example.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Report implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int reportId;
    private final String reporterId;
    private final String targetUserId; // 대상 사용자 (또는 필요 시 postId 확장)
    private final String reason;
    private final LocalDateTime createdAt = LocalDateTime.now();

    public Report(int reportId, String reporterId, String targetUserId, String reason) {
        this.reportId = reportId;
        this.reporterId = reporterId;
        this.targetUserId = targetUserId;
        this.reason = reason;
    }

    public int getReportId() { return reportId; }
    public String getReporterId() { return reporterId; }
    public String getTargetUserId() { return targetUserId; }
    public String getReason() { return reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return String.format("Report[%d] %s -> %s | %s | %s", reportId, reporterId, targetUserId, createdAt, reason);
    }
}


