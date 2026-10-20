package org.example.cunguoimoita.review.dao;

import org.example.cunguoimoita.review.model.Review;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Chạy SQL cho F8: bảng reviews, cột customers.tier_id, và tính các thuộc tính dẫn xuất
// (/avgRating, /totalSales, /ecoPoints) bằng câu SELECT — không lưu cột riêng.
public class ReviewDAO {

    // Một dòng đánh giá để hiển thị (kèm tên người viết).
    public record ReviewLine(String authorName, int rating, String content, LocalDateTime createdDate) {
    }

    // Thống kê công khai của 1 người: đọc từ view seller_public_stats (file sql/08_review.sql).
    public record RatingSummary(double avgRating, int totalSales, String tierName) {
    }

    // 10.000đ giao dịch hoàn tất = 1 ecoPoint (cả tiền mua lẫn tiền bán)
    public static final int VND_PER_POINT = 10000;

    // ===================== REVIEW =====================

    public void insert(Connection c, Review r) throws SQLException {
        r.setId(UUID.randomUUID().toString());
        String sql = "INSERT INTO reviews (id, order_id, author_id, target_user_id, rating, content, created_date)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, r.getId());
            ps.setString(2, r.getOrderId());
            ps.setString(3, r.getAuthorId());
            ps.setString(4, r.getTargetUserId());
            ps.setInt(5, r.getRating());
            ps.setString(6, r.getContent());
            ps.setObject(7, r.getCreatedDate());
            ps.executeUpdate();
        }
    }

    // Đánh giá của 1 người cho 1 đơn (để hiện lại và sửa). Chưa có → null.
    public Review findByOrderAndAuthor(Connection c, String orderId, String authorId) throws SQLException {
        String sql = "SELECT * FROM reviews WHERE order_id = ? AND author_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, orderId);
            ps.setString(2, authorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Review r = new Review();
                r.setId(rs.getString("id"));
                r.setOrderId(rs.getString("order_id"));
                r.setAuthorId(rs.getString("author_id"));
                r.setTargetUserId(rs.getString("target_user_id"));
                r.setRating(rs.getInt("rating"));
                r.setContent(rs.getString("content"));
                r.setCreatedDate(rs.getObject("created_date", LocalDateTime.class));
                return r;
            }
        }
    }

    // Sửa số sao + nhận xét của 1 đánh giá đã có.
    public void update(Connection c, Review r) throws SQLException {
        String sql = "UPDATE reviews SET rating = ?, content = ? WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, r.getRating());
            ps.setString(2, r.getContent());
            ps.setString(3, r.getId());
            ps.executeUpdate();
        }
    }

    // Người này đã đánh giá đơn này chưa? (DB cũng chặn bằng UNIQUE(order_id, author_id))
    public boolean exists(Connection c, String orderId, String authorId) throws SQLException {
        String sql = "SELECT 1 FROM reviews WHERE order_id = ? AND author_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, orderId);
            ps.setString(2, authorId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // Các đánh giá người khác dành cho userId, mới nhất trước.
    public List<ReviewLine> findByTarget(Connection c, String userId) throws SQLException {
        String sql = "SELECT u.fullname, r.rating, r.content, r.created_date"
                + " FROM reviews r JOIN users u ON u.id = r.author_id"
                + " WHERE r.target_user_id = ? ORDER BY r.created_date DESC";
        List<ReviewLine> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ReviewLine(rs.getString("fullname"), rs.getInt("rating"), rs.getString("content"),
                            rs.getObject("created_date", LocalDateTime.class)));
                }
            }
        }
        return list;
    }

    // /avgRating, /totalSales và tên hạng của 1 người.
    public RatingSummary findSummary(Connection c, String userId) throws SQLException {
        String sql = "SELECT avg_rating, total_sales, member_tier FROM seller_public_stats WHERE user_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return new RatingSummary(0, 0, null);
                }
                return new RatingSummary(rs.getDouble("avg_rating"), rs.getInt("total_sales"), rs.getString("member_tier"));
            }
        }
    }

    // ===================== ECO POINT & HẠNG =====================

    // User./ecoPoints: tổng tiền các đơn COMPLETED người này đã mua (total_amount) và đã bán (sub_total),
    // chia cho VND_PER_POINT. Tính mỗi lần cần, không lưu cột → không bao giờ lệch dữ liệu.
    public int ecoPoints(Connection c, String userId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM ("
                + "   SELECT total_amount AS amount FROM orders WHERE buyer_id = ? AND status = 'completed'"
                + "   UNION ALL"
                + "   SELECT sub_total FROM orders WHERE seller_id = ? AND status = 'completed'"
                + " ) t";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return (int) (rs.getDouble(1) / VND_PER_POINT);
            }
        }
    }

    // Customer.tierId: đổi hạng của khách.
    public void updateTier(Connection c, String userId, String tierId) throws SQLException {
        String sql = "UPDATE customers SET tier_id = ? WHERE user_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tierId);
            ps.setString(2, userId);
            ps.executeUpdate();
        }
    }
}