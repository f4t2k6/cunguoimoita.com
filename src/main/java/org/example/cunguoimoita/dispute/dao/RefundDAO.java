package org.example.cunguoimoita.dispute.dao;

import org.example.cunguoimoita.dispute.model.Refund;
import org.example.cunguoimoita.dispute.model.RefundReason;
import org.example.cunguoimoita.dispute.model.RefundStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class RefundDAO {

    public void insert(Connection c, Refund r) throws SQLException {
        String sql = "INSERT INTO refunds (id, order_id, payment_id, buyer_id, amount, reason, status, refund_date) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, r.getId());
            ps.setString(2, r.getOrderId());
            ps.setString(3, r.getPaymentId());
            ps.setString(4, r.getBuyerId());
            ps.setDouble(5, r.getAmount());
            ps.setString(6, r.getReason().dbValue());
            ps.setString(7, r.getStatus().dbValue());
            ps.setTimestamp(8, r.getRefundDate() != null ? Timestamp.valueOf(r.getRefundDate()) : new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
        }
    }

    public Refund findByOrderId(Connection c, String orderId) throws SQLException {
        String sql = "SELECT * FROM refunds WHERE order_id = ? ORDER BY refund_date DESC LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<Refund> findByBuyerId(Connection c, String buyerId) throws SQLException {
        String sql = "SELECT * FROM refunds WHERE buyer_id = ? ORDER BY refund_date DESC";
        List<Refund> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, buyerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    private Refund map(ResultSet rs) throws SQLException {
        Refund r = new Refund();
        r.setId(rs.getString("id"));
        r.setOrderId(rs.getString("order_id"));
        r.setPaymentId(rs.getString("payment_id"));
        r.setBuyerId(rs.getString("buyer_id"));
        r.setAmount(rs.getDouble("amount"));
        r.setReason(RefundReason.fromDb(rs.getString("reason")));
        r.setStatus(RefundStatus.fromDb(rs.getString("status")));

        Timestamp rDate = rs.getTimestamp("refund_date");
        if (rDate != null) r.setRefundDate(rDate.toLocalDateTime());
        return r;
    }
}
