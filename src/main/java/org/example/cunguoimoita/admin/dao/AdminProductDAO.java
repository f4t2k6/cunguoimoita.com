package org.example.cunguoimoita.admin.dao;

import org.example.cunguoimoita.admin.model.AdminProductRow;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Bài đăng phía admin. Bài chờ duyệt: status = draft, chưa có approved_by_admin_id, chưa bị từ chối. */
public class AdminProductDAO {

    private static final String SELECT = "SELECT p.id, p.name, p.description, p.seller_id, u.fullname AS seller_name,"
            + " cg.name AS category_name, p.listing_type, p.price, p.condition, p.verified_status, p.status,"
            + " p.approved_by_admin_id, p.posted_date"
            + " FROM products p"
            + " JOIN users u ON u.id = p.seller_id"
            + " JOIN categories cg ON cg.id = p.category_id";

    /**
     * @param filter pending | approved | hidden | rejected | all
     * @param keyword tên bài hoặc tên người bán, null để bỏ qua
     */
    public List<AdminProductRow> find(Connection c, String filter, String keyword) throws SQLException {
        return find(c, filter, keyword, LIST_LIMIT);
    }

    /** Số dòng tối đa của trang danh sách (nhiều hơn thì admin dùng ô tìm kiếm). */
    public static final int LIST_LIMIT = 100;

    /** @param limit số dòng tối đa (trang tổng quan chỉ lấy vài bài mới nhất) */
    public List<AdminProductRow> find(Connection c, String filter, String keyword, int limit) throws SQLException {
        String where;
        switch (filter) {
            case "approved":
                where = " WHERE p.approved_by_admin_id IS NOT NULL AND p.status <> 'hidden'";
                break;
            case "hidden":
                where = " WHERE p.status = 'hidden'";
                break;
            case "rejected":
                where = " WHERE p.verified_status = 'rejected'";
                break;
            case "all":
                where = " WHERE TRUE";
                break;
            default:
                where = " WHERE p.approved_by_admin_id IS NULL AND p.verified_status <> 'rejected' AND p.status = 'draft'";
        }
        boolean search = keyword != null && !keyword.isBlank();
        String sql = SELECT + where
                + (search ? " AND (p.name ILIKE ? OR u.fullname ILIKE ?)" : "")
                + " ORDER BY p.posted_date DESC, p.name LIMIT " + Math.max(1, limit);
        List<AdminProductRow> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            if (search) {
                String like = "%" + keyword.trim() + "%";
                ps.setString(1, like);
                ps.setString(2, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public AdminProductRow findById(Connection c, String id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT + " WHERE p.id = ? FOR UPDATE OF p")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Product.approveProduct(): admin duyệt → bài lên bảng tin (available). */
    public void approve(Connection c, String productId, String adminId) throws SQLException {
        String sql = "UPDATE products SET approved_by_admin_id = ?, status = 'available' WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, adminId);
            ps.setString(2, productId);
            ps.executeUpdate();
        }
    }

    /** Từ chối duyệt: verified_status = rejected, bài vẫn ở draft cho người bán sửa. */
    public void reject(Connection c, String productId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE products SET verified_status = 'rejected' WHERE id = ?")) {
            ps.setString(1, productId);
            ps.executeUpdate();
        }
    }

    public void updateStatus(Connection c, String productId, String status) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE products SET status = ? WHERE id = ?")) {
            ps.setString(1, status);
            ps.setString(2, productId);
            ps.executeUpdate();
        }
    }

    /** Ẩn mọi bài đang bán của một người (khi khoá tài khoản). @return số bài bị ẩn */
    public int hideAvailableOfSeller(Connection c, String sellerId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE products SET status = 'hidden' WHERE seller_id = ? AND status = 'available'")) {
            ps.setString(1, sellerId);
            return ps.executeUpdate();
        }
    }

    private AdminProductRow map(ResultSet rs) throws SQLException {
        double price = rs.getDouble("price");
        Double priceOrNull = rs.wasNull() ? null : price;
        Date posted = rs.getDate("posted_date");
        return new AdminProductRow(rs.getString("id"), rs.getString("name"), rs.getString("description"),
                rs.getString("seller_id"), rs.getString("seller_name"), rs.getString("category_name"),
                rs.getString("listing_type"), priceOrNull, rs.getString("condition"),
                rs.getString("verified_status"), rs.getString("status"), rs.getString("approved_by_admin_id"),
                posted == null ? null : posted.toLocalDate());
    }
}
