package org.example.cunguoimoita.order.dao;

import org.example.cunguoimoita.order.model.Voucher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Chạy SQL trên bảng vouchers.
public class VoucherDAO {

    // Tìm voucher theo mã người dùng nhập (vd SALE50K). Không có → null.
    public Voucher findByCode(Connection c, String code) throws SQLException {
        String sql = "SELECT * FROM vouchers WHERE code = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    // Voucher của khách còn dùng được (chưa dùng, chưa hết hạn), để trang đơn gợi ý mã.
    public List<Voucher> findUsableByCustomer(Connection c, String customerId) throws SQLException {
        String sql = "SELECT * FROM vouchers WHERE customer_id = ? AND status = 'issued'"
                + " AND (expiry_date IS NULL OR expiry_date >= CURRENT_DATE) ORDER BY discount_value DESC";
        List<Voucher> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    // 1 dòng ở trang quản lý voucher của admin: voucher + email của khách sở hữu.
    public record VoucherRow(Voucher voucher, String ownerEmail, String ownerName) {
    }

    // Voucher mới phát gần đây (trang admin). Voucher quá hạn nhưng chưa dùng hiện là "expired".
    public List<VoucherRow> findRecent(Connection c, int limit) throws SQLException {
        String sql = "SELECT v.*, u.email, u.fullname FROM vouchers v JOIN users u ON u.id = v.customer_id"
                + " ORDER BY v.issued_date DESC, v.code LIMIT ?";
        List<VoucherRow> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Voucher v = map(rs);
                    v.checkValidity();               // chỉ đổi trạng thái trong bộ nhớ để hiển thị
                    list.add(new VoucherRow(v, rs.getString("email"), rs.getString("fullname")));
                }
            }
        }
        return list;
    }

    // Đánh dấu expired mọi voucher đã quá hạn mà chưa dùng (tác vụ chạy định kỳ gọi).
    public int expireOverdue(Connection c) throws SQLException {
        String sql = "UPDATE vouchers SET status = 'expired' WHERE status = 'issued' AND expiry_date < CURRENT_DATE";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            return ps.executeUpdate();
        }
    }

    public boolean codeExists(Connection c, String code) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM vouchers WHERE code = ?")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // Phát voucher mới cho khách (admin dùng).
    public void insert(Connection c, Voucher v) throws SQLException {
        v.setId(UUID.randomUUID().toString());
        String sql = "INSERT INTO vouchers (id, code, customer_id, discount_value, min_order_value, status, expiry_date)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, v.getId());
            ps.setString(2, v.getCode());
            ps.setString(3, v.getCustomerId());
            ps.setDouble(4, v.getDiscountValue());
            ps.setDouble(5, v.getMinOrderValue());
            ps.setString(6, v.getStatus().name().toLowerCase());   // enum ISSUED → chữ "issued" trong DB
            ps.setObject(7, v.getExpiryDate());                    // setObject chấp nhận cả null
            ps.executeUpdate();
        }
    }

    // Lưu lại trạng thái sau khi Voucher.redeem() hoặc checkValidity() đổi status.
    public void updateStatus(Connection c, Voucher v) throws SQLException {
        String sql = "UPDATE vouchers SET status = ? WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, v.getStatus().name().toLowerCase());
            ps.setString(2, v.getId());
            ps.executeUpdate();
        }
    }

    // Chuyển 1 dòng ResultSet → object Voucher
    private Voucher map(ResultSet rs) throws SQLException {
        Voucher v = new Voucher();
        v.setId(rs.getString("id"));
        v.setCode(rs.getString("code"));
        v.setCustomerId(rs.getString("customer_id"));
        v.setDiscountValue(rs.getDouble("discount_value"));
        v.setMinOrderValue(rs.getDouble("min_order_value"));
        v.setStatus(Voucher.VoucherStatus.valueOf(rs.getString("status").toUpperCase()));   // "issued" → ISSUED
        v.setIssuedDate(rs.getObject("issued_date", LocalDate.class));
        v.setExpiryDate(rs.getObject("expiry_date", LocalDate.class));
        return v;
    }
}