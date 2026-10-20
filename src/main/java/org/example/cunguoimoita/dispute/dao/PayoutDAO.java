package org.example.cunguoimoita.dispute.dao;

import org.example.cunguoimoita.dispute.model.Payout;
import org.example.cunguoimoita.dispute.model.PayoutMethod;
import org.example.cunguoimoita.dispute.model.PayoutStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class PayoutDAO {

    public record SellerEarningsSummary(double totalGross, double totalCommission, double totalNet,
                                        double scheduledAmount, double completedAmount) {
    }

    public void insert(Connection c, Payout p) throws SQLException {
        String sql = "INSERT INTO payouts (id, order_id, seller_id, gross_amount, commission_fee, net_amount, method, status, payout_date) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getId());
            ps.setString(2, p.getOrderId());
            ps.setString(3, p.getSellerId());
            ps.setDouble(4, p.getGrossAmount());
            ps.setDouble(5, p.getCommissionFee());
            ps.setDouble(6, p.getNetAmount());
            ps.setString(7, p.getMethod().dbValue());
            ps.setString(8, p.getStatus().dbValue());
            ps.setTimestamp(9, p.getPayoutDate() != null ? Timestamp.valueOf(p.getPayoutDate()) : null);
            ps.executeUpdate();
        }
    }

    public void updateStatus(Connection c, String payoutId, PayoutStatus status) throws SQLException {
        String sql = "UPDATE payouts SET status = ?, payout_date = ? WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.dbValue());
            ps.setTimestamp(2, status == PayoutStatus.DONE ? new Timestamp(System.currentTimeMillis()) : null);
            ps.setString(3, payoutId);
            ps.executeUpdate();
        }
    }

    public Payout findById(Connection c, String id) throws SQLException {
        String sql = "SELECT * FROM payouts WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public Payout findByOrderId(Connection c, String orderId) throws SQLException {
        String sql = "SELECT * FROM payouts WHERE order_id = ? LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<Payout> findBySellerId(Connection c, String sellerId) throws SQLException {
        String sql = "SELECT * FROM payouts WHERE seller_id = ? ORDER BY COALESCE(payout_date, CURRENT_TIMESTAMP) DESC";
        List<Payout> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public SellerEarningsSummary getSellerSummary(Connection c, String sellerId) throws SQLException {
        String sql = "SELECT "
                   + "COALESCE(SUM(gross_amount), 0) AS total_gross, "
                   + "COALESCE(SUM(commission_fee), 0) AS total_fee, "
                   + "COALESCE(SUM(net_amount), 0) AS total_net, "
                   + "COALESCE(SUM(CASE WHEN status = 'scheduled' THEN net_amount ELSE 0 END), 0) AS scheduled_net, "
                   + "COALESCE(SUM(CASE WHEN status = 'done' THEN net_amount ELSE 0 END), 0) AS done_net "
                   + "FROM payouts WHERE seller_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new SellerEarningsSummary(
                            rs.getDouble("total_gross"),
                            rs.getDouble("total_fee"),
                            rs.getDouble("total_net"),
                            rs.getDouble("scheduled_net"),
                            rs.getDouble("done_net")
                    );
                }
                return new SellerEarningsSummary(0, 0, 0, 0, 0);
            }
        }
    }

    /** Khoản đang chờ trả mà người bán đã khai tài khoản nhận tiền → tác vụ định kỳ chuyển tiền. */
    public List<Payout> findScheduledWithAccount(Connection c) throws SQLException {
        String sql = "SELECT p.* FROM payouts p JOIN customers cu ON cu.user_id = p.seller_id"
                   + " WHERE p.status = 'scheduled' AND COALESCE(cu.payout_account_ref, '') <> ''";
        List<Payout> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    private Payout map(ResultSet rs) throws SQLException {
        Payout p = new Payout();
        p.setId(rs.getString("id"));
        p.setOrderId(rs.getString("order_id"));
        p.setSellerId(rs.getString("seller_id"));
        p.setGrossAmount(rs.getDouble("gross_amount"));
        p.setCommissionFee(rs.getDouble("commission_fee"));
        p.setNetAmount(rs.getDouble("net_amount"));
        p.setMethod(PayoutMethod.fromDb(rs.getString("method")));
        p.setStatus(PayoutStatus.fromDb(rs.getString("status")));

        Timestamp pDate = rs.getTimestamp("payout_date");
        if (pDate != null) p.setPayoutDate(pDate.toLocalDateTime());

        return p;
    }

    /** Hạng thành viên và tài khoản nhận tiền của người bán, để tính phí sàn. */
    public record SellerTier(String tierName, double discountRate, String payoutAccountRef) {
    }

    public SellerTier findSellerTier(Connection c, String sellerId) throws SQLException {
        String sql = "SELECT mt.name, mt.discount_rate, cu.payout_account_ref FROM customers cu "
                   + "JOIN member_tiers mt ON cu.tier_id = mt.id WHERE cu.user_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? new SellerTier(rs.getString("name"), rs.getDouble("discount_rate"), rs.getString("payout_account_ref"))
                        : null;
            }
        }
    }
}
