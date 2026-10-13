package org.example.cunguoimoita.notification.service;

import org.example.cunguoimoita.notification.dao.NotificationDAO;
import org.example.cunguoimoita.notification.model.Notification;
import org.example.cunguoimoita.notification.model.NotificationType;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Thông báo cho cả nhóm dùng. Ví dụ F4 báo người bán có đơn mới:
 *   new NotificationService().send(sellerId, NotificationType.ORDER, "Bạn có đơn hàng mới", "/order/detail?id=" + orderId);
 * linkUrl viết không kèm context path (bắt đầu bằng "/").
 */
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

    public int unreadCount(String userId) throws SQLException {
        return notificationDAO.countUnread(userId);
    }

    /** Đánh dấu đã đọc một thông báo và trả về link để chuyển tới (null nếu không có). */
    public String open(String userId, String notificationId) throws SQLException {
        Notification n = notificationDAO.findById(notificationId);
        if (n == null || !n.getUserId().equals(userId)) {
            return null;
        }
        notificationDAO.markRead(userId, notificationId);
        return n.getLinkUrl();
    }

    /** Notification.markAsRead(): chỉ đánh dấu đã đọc, không chuyển trang. */
    public void markRead(String userId, String notificationId) throws SQLException {
        open(userId, notificationId);
    }

    public void markAllRead(String userId) throws SQLException {
        notificationDAO.markRead(userId, null);
    }
}
