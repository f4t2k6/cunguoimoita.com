package org.example.cunguoimoita.chat.dao;

import org.example.cunguoimoita.chat.model.Message;
import org.example.cunguoimoita.chat.model.MessageType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {

    /** Thêm tin nhắn và cập nhật conversations.last_message_at trong cùng một câu lệnh (1 lần gọi DB). */
    public void insert(Connection c, Message m) throws SQLException {
        String sql = "WITH m AS (INSERT INTO messages (id, conversation_id, sender_id, type, content, sent_at, is_read)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING conversation_id, sent_at)"
                + " UPDATE conversations cv SET last_message_at = m.sent_at FROM m WHERE cv.id = m.conversation_id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, m.getId());
            ps.setString(2, m.getConversationId());
            ps.setString(3, m.getSenderId());
            ps.setString(4, m.getType().dbValue());
            ps.setString(5, m.getContent());
            ps.setTimestamp(6, Timestamp.valueOf(m.getSentAt()));
            ps.setBoolean(7, m.isRead());
            ps.executeUpdate();
        }
    }

    /**
     * Tin nhắn của cuộc trò chuyện, cũ trước mới sau. after = null để lấy hết, hoặc chỉ lấy tin mới hơn mốc này.
     * Chỉ trả tin khi userId là một trong hai người của cuộc trò chuyện (kiểm tra quyền luôn trong câu SELECT,
     * để mỗi lần chat.js hỏi tin mới chỉ tốn 1 câu lệnh).
     */
    public List<Message> findForParticipant(Connection c, String conversationId, String userId, Timestamp after)
            throws SQLException {
        String sql = "SELECT m.* FROM messages m JOIN conversations cv ON cv.id = m.conversation_id"
                + " WHERE m.conversation_id = ? AND (cv.asker_id = ? OR cv.seller_id = ?)"
                + (after == null ? "" : " AND m.sent_at > ?") + " ORDER BY m.sent_at";
        List<Message> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, conversationId);
            ps.setString(2, userId);
            ps.setString(3, userId);
            if (after != null) {
                ps.setTimestamp(4, after);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    /** markAllRead: đánh dấu đã đọc mọi tin người kia gửi cho userId trong cuộc trò chuyện. */
    public int markAllRead(Connection c, String conversationId, String readerId) throws SQLException {
        String sql = "UPDATE messages SET is_read = TRUE WHERE conversation_id = ? AND sender_id <> ? AND NOT is_read";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, conversationId);
            ps.setString(2, readerId);
            return ps.executeUpdate();
        }
    }

    public int countUnreadForUser(Connection c, String userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM messages m JOIN conversations cv ON cv.id = m.conversation_id "
                + "WHERE (cv.asker_id = ? OR cv.seller_id = ?) AND m.sender_id <> ? AND NOT m.is_read";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, userId);
            ps.setString(3, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private Message map(ResultSet rs) throws SQLException {
        Message m = new Message();
        m.setId(rs.getString("id"));
        m.setConversationId(rs.getString("conversation_id"));
        m.setSenderId(rs.getString("sender_id"));
        m.setType(MessageType.fromDb(rs.getString("type")));
        m.setContent(rs.getString("content"));
        m.setSentAt(rs.getTimestamp("sent_at").toLocalDateTime());
        m.setRead(rs.getBoolean("is_read"));
        return m;
    }
}
