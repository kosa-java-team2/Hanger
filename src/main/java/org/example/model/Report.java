package org.example.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Report 클래스
 * -------------------
 * 시스템에서 발생하는 신고(Report) 정보를 표현하는 모델 클래스.
 * <p>
 * 특징:
 * - 직렬화 가능 (DataStore 저장/로드 지원)
 * - 신고 ID, 신고자 ID, 대상 사용자 ID, 신고 사유, 생성일시를 보관
 * - 대상은 현재 사용자(User) 기준이지만, 추후 필요 시 게시글(Post) 등으로 확장 가능
 */
public class Report implements Serializable {
    private static final long serialVersionUID = 1L;

    // ===================== 필드 =====================

    /** 신고 고유 ID (DataStore에서 시퀀스를 통해 발급) */
    private final int reportId;

    /** 신고를 작성한 사용자 ID */
    private final String reporterUserId;

    /** 신고 대상 사용자 ID (추후 확장 시 postId 등으로 대체 가능) */
    private final String reportedTargetUserId;

    /** 신고 사유 (예: 부적절한 게시글, 사기 의심, 욕설 등) */
    private final String reportReason;

    /** 신고 생성 시각 (객체 생성 시 고정) */
    private final LocalDateTime createdAt = LocalDateTime.now();

    // ===================== 생성자 =====================
    /**
     * Report 객체 생성자
     *
     * @param reportId     신고 ID
     * @param reporterUserId   신고자 ID
     * @param reportedTargetUserId 신고 대상 사용자 ID
     * @param reportReason       신고 사유
     */
    public Report(int reportId, String reporterUserId, String reportedTargetUserId, String reportReason) {
        this.reportId = reportId;
        this.reporterUserId = reporterUserId;
        this.reportedTargetUserId = reportedTargetUserId;
        this.reportReason = reportReason;
    }

    // ===================== Getter 메서드 =====================
    public int getReportId() { return reportId; }
    public String getReporterUserId() { return reporterUserId; }
    public String getReportedTargetUserId() { return reportedTargetUserId; }
    public String getReportReason() { return reportReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // ===================== toString =====================
    /**
     * 신고 정보를 사람이 읽기 쉬운 문자열로 반환
     * 형식: Report[ID] 신고자 -> 대상자 | 생성시각 | 사유
     */
    @Override
    public String toString() {
        return String.format(
                "Report[%d] %s -> %s | %s | %s",
                reportId, reporterUserId, reportedTargetUserId, createdAt, reportReason
        );
    }
}