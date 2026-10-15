package org.example.cunguoimoita.account.dao;

import org.example.cunguoimoita.account.model.Address;
import org.example.cunguoimoita.common.db.ConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * CRUD địa chỉ; mọi thao tác đều khóa theo userId để không sửa địa chỉ người khác.
 * Theo database chung (00_database_chung.sql): addresses(customer_id, label, street, ward, district, city, is_default).
 * Bảng không có người nhận và số điện thoại riêng, nên lấy họ tên và số điện thoại của chủ tài khoản.
 */
public class AddressDAO {
    private static final String COLUMNS = "a.id, a.customer_id, a.label, u.fullname, u.phone, a.street, "
            + "a.ward, a.district, a.city, a.is_default";

    public List<Address> findByUserId(String userId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM addresses a JOIN users u ON u.id = a.customer_id "
                + "WHERE a.customer_id = ? ORDER BY a.is_default DESC, a.label, a.id";
        List<Address> addresses = new ArrayList<>();
        try (Connection c = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    addresses.add(map(rs));
                }
            }
        }
        return addresses;
    }

    public String add(String userId, String label, String addressLine, String ward,
                      String district, String province)
            throws SQLException {
        String id = UUID.randomUUID().toString();
        String lockUser = "SELECT id FROM users WHERE id = ? AND user_role = 'customer' FOR UPDATE";
        String countAddresses = "SELECT COUNT(*) FROM addresses WHERE customer_id = ?";
        String clearDefault = "UPDATE addresses SET is_default = FALSE WHERE customer_id = ? AND is_default = TRUE";
        String insert = "INSERT INTO addresses (id, customer_id, label, street, ward, district, city, is_default) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement lock = c.prepareStatement(lockUser)) {
                    lock.setString(1, userId);
                    try (ResultSet rs = lock.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Không tìm thấy tài khoản khách hàng.");
                        }
                    }
                }
                boolean firstAddress;
                try (PreparedStatement count = c.prepareStatement(countAddresses)) {
                    count.setString(1, userId);
                    try (ResultSet rs = count.executeQuery()) {
                        rs.next();
                        firstAddress = rs.getLong(1) == 0;
                    }
                }
                if (firstAddress) {
                    try (PreparedStatement clear = c.prepareStatement(clearDefault)) {
                        clear.setString(1, userId);
                        clear.executeUpdate();
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(insert)) {
                    ps.setString(1, id);
                    ps.setString(2, userId);
                    ps.setString(3, clean(label, "Địa chỉ mới"));
                    ps.setString(4, addressLine.trim());
                    ps.setString(5, clean(ward, null));
                    ps.setString(6, clean(district, null));
                    ps.setString(7, province.trim());
                    ps.setBoolean(8, firstAddress);
                    ps.executeUpdate();
                }
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

    public boolean update(String id, String userId, String label, String addressLine,
                          String ward, String district, String province) throws SQLException {
        String sql = "UPDATE addresses SET label = ?, street = ?, ward = ?, district = ?, city = ? "
                + "WHERE id = ? AND customer_id = ?";
        try (Connection c = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, clean(label, "Địa chỉ mới"));
            ps.setString(2, addressLine.trim());
            ps.setString(3, clean(ward, null));
            ps.setString(4, clean(district, null));
            ps.setString(5, province.trim());
            ps.setString(6, id);
            ps.setString(7, userId);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean setDefault(String id, String userId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement lock = c.prepareStatement(
                        "SELECT id FROM users WHERE id = ? AND user_role = 'customer' FOR UPDATE")) {
                    lock.setString(1, userId);
                    try (ResultSet rs = lock.executeQuery()) {
                        if (!rs.next()) {
                            c.rollback();
                            return false;
                        }
                    }
                }
                try (PreparedStatement clear = c.prepareStatement(
                        "UPDATE addresses SET is_default = FALSE WHERE customer_id = ?")) {
                    clear.setString(1, userId);
                    clear.executeUpdate();
                }
                int changed;
                try (PreparedStatement set = c.prepareStatement(
                        "UPDATE addresses SET is_default = TRUE WHERE id = ? AND customer_id = ?")) {
                    set.setString(1, id);
                    set.setString(2, userId);
                    changed = set.executeUpdate();
                }
                if (changed != 1) {
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

    public boolean delete(String id, String userId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try {
                // Khóa dòng user để hai thao tác đổi/xóa địa chỉ mặc định không chạy đè nhau.
                try (PreparedStatement lock = c.prepareStatement(
                        "SELECT id FROM users WHERE id = ? AND user_role = 'customer' FOR UPDATE")) {
                    lock.setString(1, userId);
                    try (ResultSet rs = lock.executeQuery()) {
                        if (!rs.next()) {
                            c.rollback();
                            return false;
                        }
                    }
                }

                boolean deletingDefault;
                try (PreparedStatement check = c.prepareStatement(
                        "SELECT is_default FROM addresses WHERE id = ? AND customer_id = ? FOR UPDATE")) {
                    check.setString(1, id);
                    check.setString(2, userId);
                    try (ResultSet rs = check.executeQuery()) {
                        if (!rs.next()) {
                            c.rollback();
                            return false;
                        }
                        deletingDefault = rs.getBoolean(1);
                    }
                }

                try (PreparedStatement delete = c.prepareStatement(
                        "DELETE FROM addresses WHERE id = ? AND customer_id = ?")) {
                    delete.setString(1, id);
                    delete.setString(2, userId);
                    delete.executeUpdate();
                }

                if (deletingDefault) {
                    try (PreparedStatement promote = c.prepareStatement(
                            "UPDATE addresses SET is_default = TRUE "
                                    + "WHERE id = (SELECT id FROM addresses WHERE customer_id = ? "
                                    + "ORDER BY label, id LIMIT 1)")) {
                        promote.setString(1, userId);
                        promote.executeUpdate();
                    }
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

    private Address map(ResultSet rs) throws SQLException {
        return new Address(rs.getString("id"), rs.getString("customer_id"),
                rs.getString("label"), rs.getString("fullname"),
                rs.getString("phone"), rs.getString("street"),
                rs.getString("ward"), rs.getString("district"),
                rs.getString("city"), rs.getBoolean("is_default"), null);
    }

    private String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
