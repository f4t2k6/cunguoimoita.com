package org.example.cunguoimoita.admin.dao;

import org.example.cunguoimoita.admin.model.AdminUserRow;
import org.example.cunguoimoita.common.model.AccountStatus;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Người dùng phía admin: tìm kiếm, khoá / mở khoá. */
public class AdminUserDAO {

    private static final String SELECT = "SELECT u.id, u.fullname, u.email, u.phone, u.user_role, u.status, u.join_date,"
            + " mt.name AS tier_name,"
            + " (SELECT COUNT(*) FROM products p WHERE p.seller_id = u.id) AS product_count,"
            + " (SELECT COUNT(*) FROM reports r WHERE r.target_type = 'user' AND r.target_id = u.id) AS report_count"
            + " FROM users u"
            + " LEFT JOIN customers cu ON cu.user_id = u.id"
            + " LEFT JOIN member_tiers mt ON mt.id = cu.tier_id";

    /**
     * @param keyword tên, email hoặc số điện thoại, null để lấy tất cả
     * @param status active | banned | null
     */
    public List<AdminUserRow> search(Connection c, String keyword, String status) throws SQLException {
        boolean search = keyword != null && !keyword.isBlank();
        boolean byStatus = "active".equals(status) || "banned".equals(status);
        String sql = SELECT + " WHERE TRUE"
                + (search ? " AND (u.fullname ILIKE ? OR u.email ILIKE ? OR u.phone ILIKE ?)" : "")
                + (byStatus ? " AND u.status = ?" : "")
                + " ORDER BY u.user_role, u.join_date DESC, u.fullname LIMIT 100";
        List<AdminUserRow> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            if (search) {
                String like = "%" + keyword.trim() + "%";
                ps.setString(i++, like);
                ps.setString(i++, like);
                ps.setString(i++, like);
            }
            if (byStatus) {
                ps.setString(i, status);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public AdminUserRow findById(Connection c, String id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT + " WHERE u.id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** User.banUser() / mở khoá: status = banned | active. */
    public void updateStatus(Connection c, String userId, AccountStatus status) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE users SET status = ? WHERE id = ?")) {
            ps.setString(1, status.databaseValue());
            ps.setString(2, userId);
            ps.executeUpdate();
        }
    }

    private AdminUserRow map(ResultSet rs) throws SQLException {
        Date joined = rs.getDate("join_date");
        return new AdminUserRow(rs.getString("id"), rs.getString("fullname"), rs.getString("email"),
                rs.getString("phone"), rs.getString("user_role"), rs.getString("status"),
                joined == null ? null : joined.toLocalDate(), rs.getString("tier_name"),
                rs.getInt("product_count"), rs.getInt("report_count"));
    }
}
