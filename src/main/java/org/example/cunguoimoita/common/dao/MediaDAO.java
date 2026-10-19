package org.example.cunguoimoita.common.dao;

import org.example.cunguoimoita.common.db.ConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/** Bảng media: lưu nội dung ảnh để mọi máy chạy Tomcat đều đọc được (dùng ConnectionPool + PreparedStatement). */
public class MediaDAO {

    /** Một ảnh đọc từ database. */
    public record Media(String contentType, byte[] data) {
    }

    /** Lưu ảnh, trả về id mới. */
    public String insert(String ownerId, String contentType, byte[] data) throws SQLException {
        String id = UUID.randomUUID().toString();
        String sql = "INSERT INTO media (id, owner_id, content_type, data, size_bytes) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, ownerId);
            ps.setString(3, contentType);
            ps.setBytes(4, data);
            ps.setInt(5, data.length);
            ps.executeUpdate();
        }
        return id;
    }

    public Media find(String id) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT content_type, data FROM media WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Media(rs.getString(1), rs.getBytes(2)) : null;
            }
        }
    }

    public void delete(String id) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM media WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }
}