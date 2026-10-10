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

    public Notification findById(String id) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM notifications WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public int countUnread(String userId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND NOT is_read")) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** id = null thì đánh dấu đã đọc tất cả của user. */
    public void markRead(String userId, String id) throws SQLException {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ?" + (id == null ? "" : " AND id = ?");
        try (Connection c = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            if (id != null) {
                ps.setString(2, id);
            }
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
