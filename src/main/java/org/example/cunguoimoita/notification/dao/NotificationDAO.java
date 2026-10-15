package org.example.cunguoimoita.notification.dao;

import org.example.cunguoimoita.common.db.ConnectionPool;
import org.example.cunguoimoita.notification.model.Notification;
import org.example.cunguoimoita.notification.model.NotificationType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {

    public void insert(Notification n) throws SQLException {
        String sql = "INSERT INTO notifications (id, user_id, type, content, link_url, is_read, created_date) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, n.getId());
            ps.setString(2, n.getUserId());
            ps.setString(3, n.getType().dbValue());
            ps.setString(4, n.getContent());
            ps.setString(5, n.getLinkUrl());
            ps.setBoolean(6, n.isRead());
            ps.setTimestamp(7, Timestamp.valueOf(n.getCreatedDate()));
            ps.executeUpdate();
        }
    }

    public List<Notification> findByUser(String userId, int limit) throws SQLException {
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_date DESC LIMIT ?";
        List<Notification> list = new ArrayList<>();
        try (Connection c = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    /**
     * Số thông báo chưa đọc và số tin nhắn chưa đọc (F7) trong 1 câu lệnh,
     * để common.js cập nhật cả 2 con số trên header chỉ với 1 lần gọi server.
     * @return {thông báo chưa đọc, tin nhắn chưa đọc}
     */
    public int[] countBadges(String userId) throws SQLException {
        String sql = "SELECT (SELECT COUNT(*) FROM notifications WHERE user_id = ? AND NOT is_read),"
                + " (SELECT COUNT(*) FROM messages m JOIN conversations cv ON cv.id = m.conversation_id"
                + "   WHERE (cv.asker_id = ? OR cv.seller_id = ?) AND m.sender_id <> ? AND NOT m.is_read)";
        try (Connection c = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 1; i <= 4; i++) {
                ps.setString(i, userId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new int[]{rs.getInt(1), rs.getInt(2)};
            }
        }
    }

    /**
     * Đánh dấu đã đọc một thông báo của user và trả về linkUrl của nó trong cùng một câu lệnh.
     * @return linkUrl, hoặc null nếu thông báo không tồn tại / không phải của user
     */
    public String markRead(String userId, String id) throws SQLException {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND id = ? RETURNING link_url";
        try (Connection c = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    public void markAllRead(String userId) throws SQLException {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND NOT is_read";
        try (Connection c = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.executeUpdate();
        }
    }

    private Notification map(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getString("id"));
        n.setUserId(rs.getString("user_id"));
        n.setType(NotificationType.fromDb(rs.getString("type")));
        n.setContent(rs.getString("content"));
        n.setLinkUrl(rs.getString("link_url"));
        n.setRead(rs.getBoolean("is_read"));
        n.setCreatedDate(rs.getTimestamp("created_date").toLocalDateTime());
        return n;
    }
}
