package org.example.cunguoimoita.account.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;

/** Đăng ký đang chờ xác nhận email; tài khoản trong users chỉ được tạo khi nhập đúng mã. */
public class EmailVerificationDAO {

    /** Một dòng đang chờ. */
    public record Pending(String email, String fullname, String phone, String passwordHash, String codeHash,
                          OffsetDateTime expiresAt, int attempts, OffsetDateTime lastSentAt) {
    }

    public Pending findForUpdate(Connection c, String email) throws SQLException {
        String sql = "SELECT email, fullname, phone, password_hash, code_hash, expires_at, attempts, last_sent_at "
                + "FROM email_verifications WHERE email = ? FOR UPDATE";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new Pending(rs.getString("email"), rs.getString("fullname"), rs.getString("phone"),
                        rs.getString("password_hash"), rs.getString("code_hash"),
                        rs.getObject("expires_at", OffsetDateTime.class), rs.getInt("attempts"),
                        rs.getObject("last_sent_at", OffsetDateTime.class));
            }
        }
    }

    /** Thêm mới hoặc ghi đè lần đăng ký trước của cùng email (mã mới, đếm lại số lần nhập). */
    public void upsert(Connection c, String email, String fullname, String phone, String passwordHash,
                       String codeHash, int validMinutes) throws SQLException {
        String sql = "INSERT INTO email_verifications "
                + "(email, fullname, phone, password_hash, code_hash, expires_at, attempts, last_sent_at) "
                + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP + make_interval(mins => ?), 0, CURRENT_TIMESTAMP) "
                + "ON CONFLICT (email) DO UPDATE SET fullname = EXCLUDED.fullname, phone = EXCLUDED.phone, "
                + "password_hash = EXCLUDED.password_hash, code_hash = EXCLUDED.code_hash, "
                + "expires_at = EXCLUDED.expires_at, attempts = 0, last_sent_at = EXCLUDED.last_sent_at";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, fullname);
            ps.setString(3, phone);
            ps.setString(4, passwordHash);
            ps.setString(5, codeHash);
            ps.setInt(6, validMinutes);
            ps.executeUpdate();
        }
    }

    /** Gửi lại mã: chỉ đổi mã và hạn, giữ thông tin đăng ký. */
    public void renewCode(Connection c, String email, String codeHash, int validMinutes) throws SQLException {
        String sql = "UPDATE email_verifications SET code_hash = ?, attempts = 0, "
                + "expires_at = CURRENT_TIMESTAMP + make_interval(mins => ?), last_sent_at = CURRENT_TIMESTAMP "
                + "WHERE email = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, codeHash);
            ps.setInt(2, validMinutes);
            ps.setString(3, email);
            ps.executeUpdate();
        }
    }

    public void addAttempt(Connection c, String email) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE email_verifications SET attempts = attempts + 1 WHERE email = ?")) {
            ps.setString(1, email);
            ps.executeUpdate();
        }
    }

    public void delete(Connection c, String email) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM email_verifications WHERE email = ?")) {
            ps.setString(1, email);
            ps.executeUpdate();
        }
    }

    /** Dọn các lượt đăng ký đã hết hạn quá 1 ngày (gọi kèm mỗi lần có người đăng ký). */
    public void deleteExpired(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "DELETE FROM email_verifications WHERE expires_at < CURRENT_TIMESTAMP - INTERVAL '1 day'")) {
            ps.executeUpdate();
        }
    }
}
