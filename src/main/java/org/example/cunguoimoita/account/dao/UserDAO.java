package org.example.cunguoimoita.account.dao;

import org.example.cunguoimoita.account.model.Customer;
import org.example.cunguoimoita.account.model.User;
import org.example.cunguoimoita.common.db.ConnectionPool;
import org.example.cunguoimoita.common.model.AccountStatus;
import org.example.cunguoimoita.common.model.UserRole;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
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
