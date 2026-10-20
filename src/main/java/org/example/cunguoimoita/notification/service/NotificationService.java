package org.example.cunguoimoita.notification.service;

import org.example.cunguoimoita.notification.dao.NotificationDAO;
import org.example.cunguoimoita.notification.model.Notification;
import org.example.cunguoimoita.notification.model.NotificationType;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Gửi thông báo cho người dùng; linkUrl không kèm context path. */
public class NotificationService {

    private final NotificationDAO notificationDAO = new NotificationDAO();

    /** Notification.send() */
    public Notification send(String userId, NotificationType type, String content, String linkUrl) throws SQLException {
        Notification n = new Notification();
        n.setId(UUID.randomUUID().toString());
        n.setUserId(userId);
        n.setType(type);
        n.setContent(content);
        n.setLinkUrl(linkUrl);
        n.setCreatedDate(LocalDateTime.now());
        notificationDAO.insert(n);
        return n;
    }

    public List<Notification> list(String userId) throws SQLException {
        return notificationDAO.findByUser(userId, 50);
    }

    /** limit thông báo mới nhất (danh sách xổ xuống ở header lấy 20). */
    public List<Notification> list(String userId, int limit) throws SQLException {
        return notificationDAO.findByUser(userId, limit);
    }

    /** {thông báo chưa đọc, tin nhắn chưa đọc} cho 2 con số trên header. */
    public int[] badgeCounts(String userId) throws SQLException {
        return notificationDAO.countBadges(userId);
    }

    /** Đánh dấu đã đọc một thông báo và trả về link để chuyển tới (null nếu không có). */
    public String open(String userId, String notificationId) throws SQLException {
        return notificationId == null ? null : notificationDAO.markRead(userId, notificationId);
    }

    /** Notification.markAsRead(): chỉ đánh dấu đã đọc, không chuyển trang. */
    public void markRead(String userId, String notificationId) throws SQLException {
        open(userId, notificationId);
    }

    public void markAllRead(String userId) throws SQLException {
        notificationDAO.markAllRead(userId);
    }
}
