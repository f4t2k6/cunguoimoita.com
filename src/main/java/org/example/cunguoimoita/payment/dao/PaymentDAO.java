package org.example.cunguoimoita.payment.dao;

import org.example.cunguoimoita.payment.model.Payment;
import org.example.cunguoimoita.payment.model.PaymentGateway;
import org.example.cunguoimoita.payment.model.PaymentStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Quản lý đọc/ghi bảng payments trong cơ sở dữ liệu.
 * Mọi hàm nhận Connection để chạy chung transaction với PaymentService / OrderService.
 */
public class PaymentDAO {

    public void insert(Connection c, Payment p) throws SQLException {
        String sql = "INSERT INTO payments (id, code, order_id, payer_id, amount, gateway, gateway_txn_id, status, created_at, paid_at) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getId());
            ps.setString(2, p.getCode());
            ps.setString(3, p.getOrderId());
            ps.setString(4, p.getPayerId());
            ps.setDouble(5, p.getAmount());
            ps.setString(6, p.getGateway().dbValue());
            ps.setString(7, p.getGatewayTxnId());
            ps.setString(8, p.getStatus().dbValue());
            ps.setTimestamp(9, p.getCreatedAt() != null ? Timestamp.valueOf(p.getCreatedAt()) : new Timestamp(System.currentTimeMillis()));
            ps.setTimestamp(10, p.getPaidAt() != null ? Timestamp.valueOf(p.getPaidAt()) : null);
            ps.executeUpdate();
        }
    }

    public void updateStatus(Connection c, String paymentId, PaymentStatus status, String gatewayTxnId) throws SQLException {
        String sql = "UPDATE payments SET status = ?, gateway_txn_id = ?, paid_at = ? WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.dbValue());
            ps.setString(2, gatewayTxnId);
            if (status == PaymentStatus.PAID) {
                ps.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            } else {
                ps.setTimestamp(3, null);
            }
            ps.setString(4, paymentId);
            ps.executeUpdate();
        }
    }

    public Payment findById(Connection c, String id) throws SQLException {
        String sql = "SELECT * FROM payments WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public Payment findByOrderId(Connection c, String orderId) throws SQLException {
        String sql = "SELECT * FROM payments WHERE order_id = ? ORDER BY created_at DESC LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Giống findById nhưng khoá dòng thanh toán tới hết transaction (tránh 2 lần xác nhận cùng lúc). */
    public Payment findByIdForUpdate(Connection c, String id) throws SQLException {
        String sql = "SELECT * FROM payments WHERE id = ? FOR UPDATE";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Khoản đã thanh toán thành công của đơn (để hoàn tiền). Không có → null. */
    public Payment findPaidByOrderId(Connection c, String orderId) throws SQLException {
        String sql = "SELECT * FROM payments WHERE order_id = ? AND status = 'paid' ORDER BY paid_at DESC LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Lịch sử thanh toán của 1 đơn, mới nhất trước (hiện ở trang chi tiết đơn). */
    public List<Payment> findAllByOrderId(Connection c, String orderId) throws SQLException {
        String sql = "SELECT * FROM payments WHERE order_id = ? ORDER BY created_at DESC";
        List<Payment> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    /** Huỷ các lần thanh toán còn đang chờ của đơn (người mua bắt đầu lần mới hoặc đổi sang COD). */
    public void failPendingByOrderId(Connection c, String orderId) throws SQLException {
        String sql = "UPDATE payments SET status = 'failed' WHERE order_id = ? AND status = 'pending'";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, orderId);
            ps.executeUpdate();
        }
    }

    public Payment findByCode(Connection c, String code) throws SQLException {
        String sql = "SELECT * FROM payments WHERE code = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<Payment> findByPayerId(Connection c, String payerId, int limit) throws SQLException {
        String sql = "SELECT * FROM payments WHERE payer_id = ? ORDER BY created_at DESC LIMIT ?";
        List<Payment> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, payerId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    private Payment map(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setId(rs.getString("id"));
        p.setCode(rs.getString("code"));
        p.setOrderId(rs.getString("order_id"));
        p.setPayerId(rs.getString("payer_id"));
        p.setAmount(rs.getDouble("amount"));
        p.setGateway(PaymentGateway.fromDb(rs.getString("gateway")));
        p.setGatewayTxnId(rs.getString("gateway_txn_id"));
        p.setStatus(PaymentStatus.fromDb(rs.getString("status")));

        Timestamp cAt = rs.getTimestamp("created_at");
        if (cAt != null) p.setCreatedAt(cAt.toLocalDateTime());

        Timestamp pAt = rs.getTimestamp("paid_at");
        if (pAt != null) p.setPaidAt(pAt.toLocalDateTime());

        return p;
    }
}
