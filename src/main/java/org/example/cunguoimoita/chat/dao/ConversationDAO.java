package org.example.cunguoimoita.chat.dao;

import org.example.cunguoimoita.chat.model.Conversation;
import org.example.cunguoimoita.chat.model.ConversationStatus;
import org.example.cunguoimoita.product.model.ProductCondition;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** Các hàm nhận Connection từ ChatService để chạy chung một transaction khi cần. */
public class ConversationDAO {

    /** Kèm tên, ảnh đại diện 2 người (users) và tên, giá, ảnh, tình trạng món (products) để hiển thị. */
    private static final String COLUMNS = "SELECT cv.*, ua.fullname AS asker_name, us.fullname AS seller_name,"
            + " ua.avatar_url AS asker_avatar, us.avatar_url AS seller_avatar,"
            + " p.name AS product_name, p.price AS product_price, p.images AS product_images,"
            + " p.condition AS product_condition";
    private static final String FROM = " FROM conversations cv"
            + " LEFT JOIN users ua ON ua.id = cv.asker_id"
            + " LEFT JOIN users us ON us.id = cv.seller_id"
            + " LEFT JOIN products p ON p.id = cv.product_id";
    private static final String SELECT = COLUMNS + FROM;

    public void insert(Connection c, Conversation conv) throws SQLException {
        String sql = "INSERT INTO conversations (id, product_id, asker_id, seller_id, status, created_at, last_message_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, conv.getId());
            ps.setString(2, conv.getProductId());
            ps.setString(3, conv.getAskerId());
            ps.setString(4, conv.getSellerId());
            ps.setString(5, conv.getStatus().dbValue());
            ps.setTimestamp(6, Timestamp.valueOf(conv.getCreatedAt()));
            ps.setTimestamp(7, conv.getLastMessageAt() == null ? null : Timestamp.valueOf(conv.getLastMessageAt()));
            ps.executeUpdate();
        }
    }

    public Conversation findById(Connection c, String id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT + " WHERE cv.id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public Conversation findByProductAndAsker(Connection c, String productId, String askerId) throws SQLException {
        String sql = SELECT + " WHERE cv.product_id = ? AND cv.asker_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, productId);
            ps.setString(2, askerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /**
     * Hộp thư: các cuộc trò chuyện của user, mới nhất trước, kèm tin cuối và số tin chưa đọc.
     * Dùng LEFT JOIN LATERAL: mỗi cuộc trò chuyện đọc tin cuối 1 lần (theo idx_messages_conversation)
     * thay vì 3 câu SELECT con như trước.
     */
    public List<Conversation> findInbox(Connection c, String userId) throws SQLException {
        String sql = COLUMNS + ", lm.content AS last_content, lm.type AS last_type, un.unread" + FROM
                + " LEFT JOIN LATERAL (SELECT m.content, m.type FROM messages m WHERE m.conversation_id = cv.id"
                + "   ORDER BY m.sent_at DESC LIMIT 1) lm ON TRUE"
                + " LEFT JOIN LATERAL (SELECT COUNT(*) AS unread FROM messages m WHERE m.conversation_id = cv.id"
                + "   AND m.sender_id <> ? AND NOT m.is_read) un ON TRUE"
                + " WHERE cv.asker_id = ? OR cv.seller_id = ?"
                + " ORDER BY COALESCE(cv.last_message_at, cv.created_at) DESC";
        List<Conversation> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, userId);
            ps.setString(3, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Conversation conv = map(rs);
                    String type = rs.getString("last_type");
                    conv.setLastMessagePreview(type == null ? "" :
                            "price_offer".equals(type) ? "[Trả giá]" :
                            "image".equals(type) ? "[Hình ảnh]" : rs.getString("last_content"));
                    conv.setUnreadCount(rs.getInt("unread"));
                    list.add(conv);
                }
            }
        }
        return list;
    }

    public void updateStatus(Connection c, Conversation conv) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE conversations SET status = ? WHERE id = ?")) {
            ps.setString(1, conv.getStatus().dbValue());
            ps.setString(2, conv.getId());
            ps.executeUpdate();
        }
    }

    private Conversation map(ResultSet rs) throws SQLException {
        Conversation conv = new Conversation();
        conv.setId(rs.getString("id"));
        conv.setProductId(rs.getString("product_id"));
        conv.setAskerId(rs.getString("asker_id"));
        conv.setSellerId(rs.getString("seller_id"));
        conv.setStatus(ConversationStatus.fromDb(rs.getString("status")));
        conv.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        Timestamp last = rs.getTimestamp("last_message_at");
        conv.setLastMessageAt(last == null ? null : last.toLocalDateTime());
        conv.setAskerName(rs.getString("asker_name"));
        conv.setSellerName(rs.getString("seller_name"));
        conv.setAskerAvatar(rs.getString("asker_avatar"));
        conv.setSellerAvatar(rs.getString("seller_avatar"));
        conv.setProductName(rs.getString("product_name"));
        double price = rs.getDouble("product_price");
        conv.setProductPrice(rs.wasNull() ? null : price);
        // products.images lưu nhiều URL cách nhau bởi dấu phẩy, chỉ cần ảnh đầu để làm ảnh bìa
        String images = rs.getString("product_images");
        if (images != null && !images.isBlank()) {
            conv.setProductImage(images.split(",")[0].trim());
        }
        conv.setProductCondition(ProductCondition.fromCode(rs.getString("product_condition")));
        return conv;
    }
}
