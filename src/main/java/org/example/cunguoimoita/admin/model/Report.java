package org.example.cunguoimoita.admin.model;

import java.time.LocalDateTime;

/** Class Report trong class diagram: người dùng báo cáo bài đăng hoặc người dùng vi phạm. */
public class Report {

    private String id;
    private String reporterId;
    private String targetType;      // product | user
    private String targetId;
    private ReportReason reason;
    private String description;
    private String evidenceImages;
    private String status;          // pending | resolved | rejected
    private LocalDateTime createdDate;
    private String handledByAdminId;

    // Chỉ để hiển thị trên trang admin (lấy kèm bằng JOIN)
    private String reporterName;
    private String targetName;      // tên bài đăng hoặc tên người bị báo cáo
    private String targetOwnerId;   // người bán của bài, hoặc chính người bị báo cáo

    public boolean isPending() { return "pending".equals(status); }
    public boolean isProductTarget() { return "product".equals(targetType); }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getReporterId() { return reporterId; }
    public void setReporterId(String reporterId) { this.reporterId = reporterId; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public ReportReason getReason() { return reason; }
    public void setReason(ReportReason reason) { this.reason = reason; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getEvidenceImages() { return evidenceImages; }
    public void setEvidenceImages(String evidenceImages) { this.evidenceImages = evidenceImages; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public String getHandledByAdminId() { return handledByAdminId; }
    public void setHandledByAdminId(String handledByAdminId) { this.handledByAdminId = handledByAdminId; }
    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }
    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
    public String getTargetOwnerId() { return targetOwnerId; }
    public void setTargetOwnerId(String targetOwnerId) { this.targetOwnerId = targetOwnerId; }
}
