package org.example.cunguoimoita.notification.model;

import java.time.LocalDateTime;
import java.time.ZoneId;

/** Class Notification trong class diagram. send() nằm trong NotificationService.send(). */
public class Notification {

    private String id;
    private String userId;
    private NotificationType type;
    private String content;
    private String linkUrl;
    private boolean isRead;
    private LocalDateTime createdDate;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getLinkUrl() { return linkUrl; }
    public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    /** Mốc thời gian dạng số (ms) để JS hiển thị "5 phút trước". */
    public long getCreatedEpochMs() {
        return createdDate == null ? 0 : createdDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
