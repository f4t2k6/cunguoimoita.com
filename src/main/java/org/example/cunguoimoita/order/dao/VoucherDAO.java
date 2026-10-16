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

    // Danh sách voucher của 1 khách (để hiện ở trang thanh toán).
    public List<Voucher> findByCustomer(Connection c, String customerId) throws SQLException {
        String sql = "SELECT * FROM vouchers WHERE customer_id = ? ORDER BY issued_date DESC";
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