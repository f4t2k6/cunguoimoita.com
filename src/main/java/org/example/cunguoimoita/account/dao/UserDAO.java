package org.example.cunguoimoita.account.dao;

import org.example.cunguoimoita.account.model.Customer;
import org.example.cunguoimoita.account.model.User;
import org.example.cunguoimoita.account.service.SocialLoginException;
import org.example.cunguoimoita.common.db.ConnectionPool;
import org.example.cunguoimoita.common.model.AccountStatus;
import org.example.cunguoimoita.common.model.UserRole;
import org.example.cunguoimoita.common.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
<<<<<<< Updated upstream
=======
import java.time.ZoneId;
import java.util.Locale;
>>>>>>> Stashed changes
import java.util.UUID;

/** DAO F1: mọi truy vấn đều dùng PreparedStatement và ConnectionPool chung. */
public class UserDAO {
    private static final String SELECT_USER = "SELECT u.id, u.email, u.password_hash, u.user_role, "
            + "u.status, u.full_name, u.phone, u.avatar_path, u.created_at, c.tier_id, c.payout_account_ref "
            + "FROM users u LEFT JOIN customers c ON c.user_id = u.id ";

    public User findByEmail(String email) throws SQLException {
        String sql = SELECT_USER + "WHERE LOWER(u.email) = LOWER(?)";
        try (Connection c = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapUser(rs) : null;
            }
        }
    }

    public User findById(String id) throws SQLException {
        String sql = SELECT_USER + "WHERE u.id = ?";
        try (Connection c = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapUser(rs) : null;
            }
        }
    }

    public Customer findCustomerById(String id) throws SQLException {
        User user = findById(id);
        if (user == null || user.getRole() != UserRole.CUSTOMER) {
            return null;
        }
        return (Customer) user;
    }

    /** Đăng ký luôn tạo customer; role không được lấy từ form. */
    public String registerCustomer(String email, String passwordHash,
                                   String fullName, String phone) throws SQLException {
        String id = UUID.randomUUID().toString();
        String insertUser = "INSERT INTO users (id, email, password_hash, user_role, status, full_name, phone) "
                + "VALUES (?, ?, ?, 'customer', 'active', ?, ?)";
        String insertCustomer = "INSERT INTO customers (user_id) VALUES (?)";

        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement userPs = c.prepareStatement(insertUser);
                 PreparedStatement customerPs = c.prepareStatement(insertCustomer)) {
                userPs.setString(1, id);
                userPs.setString(2, email.trim().toLowerCase());
                userPs.setString(3, passwordHash);
                userPs.setString(4, fullName.trim());
                userPs.setString(5, phone.trim());
                userPs.executeUpdate();

                customerPs.setString(1, id);
                customerPs.executeUpdate();
                c.commit();
                return id;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /** Tìm hoặc tạo customer liên kết với tài khoản Google/Facebook trong cùng transaction. */
    public User findOrCreateSocialCustomer(String provider, String providerUserId,
                                           String email, boolean emailVerified,
                                           String fullName, String avatarUrl)
            throws SQLException, SocialLoginException {
        provider = provider == null ? "" : provider.toLowerCase(Locale.ROOT);
        email = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        providerUserId = providerUserId == null ? "" : providerUserId.trim();
        if (!("google".equals(provider) || "facebook".equals(provider))
                || providerUserId.isBlank() || providerUserId.length() > 255
                || email.isBlank() || email.length() > 150) {
            throw new SocialLoginException("Nhà cung cấp không trả về email hoặc thông tin tài khoản hợp lệ.");
        }

        String findSocial = SELECT_USER + "JOIN social_accounts sa ON sa.user_id = u.id "
                + "WHERE sa.provider = ? AND sa.provider_user_id = ? FOR UPDATE OF u";
        String findEmail = SELECT_USER + "WHERE LOWER(u.email) = LOWER(?) FOR UPDATE OF u";
        String insertUser = "INSERT INTO users (id, fullname, email, password_hash, phone, avatar_url, user_role, status) "
                + "VALUES (?, ?, ?, ?, NULL, ?, 'customer', 'active')";
        String insertCustomer = "INSERT INTO customers (user_id, tier_id) "
                + "SELECT ?, id FROM member_tiers ORDER BY min_eco_points LIMIT 1";
        String linkSocial = "INSERT INTO social_accounts (id, provider, provider_user_id, user_id) VALUES (?, ?, ?, ?)";

        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(findSocial)) {
                    ps.setString(1, provider);
                    ps.setString(2, providerUserId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            User user = mapUser(rs);
                            if (user.getStatus() == AccountStatus.BANNED) {
                                throw new SocialLoginException("Tài khoản của bạn đang bị khóa.");
                            }
                            if (user.getRole() != UserRole.CUSTOMER) {
                                throw new SocialLoginException("Tài khoản quản trị không đăng nhập bằng mạng xã hội.");
                            }
                            c.commit();
                            return user;
                        }
                    }
                }

                try (PreparedStatement ps = c.prepareStatement(findEmail)) {
                    ps.setString(1, email);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            User existing = mapUser(rs);
                            // Chỉ Google xác nhận email_verified. Facebook không cung cấp cờ này,
                            // nên không tự liên kết email trùng với tài khoản mật khẩu hiện hữu.
                            if (!"google".equals(provider) || !emailVerified
                                    || existing.getRole() != UserRole.CUSTOMER
                                    || existing.getStatus() != AccountStatus.ACTIVE) {
                                throw new SocialLoginException("Email đã có tài khoản. Hãy đăng nhập bằng mật khẩu trước.");
                            }
                            insertSocial(c, linkSocial, provider, providerUserId, existing.getId());
                            c.commit();
                            return existing;
                        }
                    }
                }

                String id = UUID.randomUUID().toString();
                String name = fullName == null || fullName.isBlank() ? "Người dùng mới" : fullName.trim();
                if (name.length() > 100) {
                    name = name.substring(0, 100);
                }
                String placeholderHash = PasswordUtil.hashPassword(UUID.randomUUID() + UUID.randomUUID().toString());
                try (PreparedStatement ps = c.prepareStatement(insertUser)) {
                    ps.setString(1, id);
                    ps.setString(2, name);
                    ps.setString(3, email);
                    ps.setString(4, placeholderHash);
                    ps.setString(5, avatarUrl == null || avatarUrl.isBlank() ? null : avatarUrl);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(insertCustomer)) {
                    ps.setString(1, id);
                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("Chưa có hạng thành viên trong member_tiers.");
                    }
                }
                insertSocial(c, linkSocial, provider, providerUserId, id);
                try (PreparedStatement ps = c.prepareStatement(SELECT_USER + "WHERE u.id = ?")) {
                    ps.setString(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Không đọc lại được tài khoản vừa tạo.");
                        }
                        User created = mapUser(rs);
                        c.commit();
                        return created;
                    }
                }
            } catch (SQLException | SocialLoginException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    private void insertSocial(Connection c, String sql, String provider, String providerUserId,
                              String userId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, provider);
            ps.setString(3, providerUserId);
            ps.setString(4, userId);
            ps.executeUpdate();
        }
    }

    public boolean updateProfile(String id, String fullName, String phone) throws SQLException {
        String sql = "UPDATE users SET full_name = ?, phone = ?, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND user_role = 'customer' AND status = 'active'";
        try (Connection c = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, fullName.trim());
            ps.setString(2, phone.trim());
            ps.setString(3, id);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean updateCustomerProfile(String id, String fullName, String phone,
                                         String payoutAccountRef) throws SQLException {
        String updateUser = "UPDATE users SET full_name = ?, phone = ?, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND user_role = 'customer' AND status = 'active'";
        String updateCustomer = "UPDATE customers SET payout_account_ref = ? WHERE user_id = ?";
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try {
                int userRows;
                try (PreparedStatement ps = c.prepareStatement(updateUser)) {
                    ps.setString(1, fullName.trim());
                    ps.setString(2, phone.trim());
                    ps.setString(3, id);
                    userRows = ps.executeUpdate();
                }
                int customerRows;
                try (PreparedStatement ps = c.prepareStatement(updateCustomer)) {
                    ps.setString(1, payoutAccountRef == null || payoutAccountRef.isBlank()
                            ? null : payoutAccountRef.trim());
                    ps.setString(2, id);
                    customerRows = ps.executeUpdate();
                }
                if (userRows != 1 || customerRows != 1) {
                    c.rollback();
                    return false;
                }
                c.commit();
                return true;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    public boolean updatePasswordHash(String id, String newPasswordHash) throws SQLException {
        String sql = "UPDATE users SET password_hash = ?, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND status = 'active'";
        try (Connection c = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setString(2, id);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean updateAvatarPath(String id, String avatarPath) throws SQLException {
        String sql = "UPDATE users SET avatar_path = ?, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND user_role = 'customer' AND status = 'active'";
        try (Connection c = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, avatarPath);
            ps.setString(2, id);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean updatePayoutAccountRef(String id, String payoutAccountRef) throws SQLException {
        String sql = "UPDATE customers SET payout_account_ref = ? WHERE user_id = ?";
        try (Connection c = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, payoutAccountRef == null || payoutAccountRef.isBlank()
                    ? null : payoutAccountRef.trim());
            ps.setString(2, id);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean isActive(String id) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE id = ? AND status = 'active'";
        try (Connection c = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean isActiveAdmin(String id) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE id = ? AND user_role = 'admin' AND status = 'active'";
        try (Connection c = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String email = rs.getString("email");
        String passwordHash = rs.getString("password_hash");
        UserRole role = UserRole.fromDatabase(rs.getString("user_role"));
        AccountStatus status = AccountStatus.fromDatabase(rs.getString("status"));
        String fullName = rs.getString("full_name");
        String phone = rs.getString("phone");
        String avatarPath = rs.getString("avatar_path");
        OffsetDateTime createdAt = rs.getObject("created_at", OffsetDateTime.class);

        if (role == UserRole.CUSTOMER) {
            return new Customer(id, email, passwordHash, role, status, fullName,
                    phone, avatarPath, createdAt, rs.getString("tier_id"),
                    rs.getString("payout_account_ref"));
        }
        return new User(id, email, passwordHash, role, status, fullName,
                phone, avatarPath, createdAt);
    }
}
