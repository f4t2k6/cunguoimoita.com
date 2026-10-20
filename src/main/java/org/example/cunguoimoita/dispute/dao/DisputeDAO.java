package org.example.cunguoimoita.dispute.dao;

import org.example.cunguoimoita.dispute.model.Dispute;
import org.example.cunguoimoita.dispute.model.DisputeReason;
import org.example.cunguoimoita.dispute.model.DisputeStatus;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class DisputeDAO {

    public void insert(Connection c, Dispute d) throws SQLException {
        String sql = "INSERT INTO disputes (id, order_id, raised_by_id, against_id, reason, description, evidence_images, "
                   + "status, created_date, resolved_date, handled_by_admin_id, resolution_note) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, d.getId());
            ps.setString(2, d.getOrderId());
            ps.setString(3, d.getRaisedById());
            ps.setString(4, d.getAgainstId());
            ps.setString(5, d.getReason().dbValue());
            ps.setString(6, d.getDescription());
            ps.setString(7, d.getEvidenceImages());
            ps.setString(8, d.getStatus().dbValue());
            ps.setDate(9, d.getCreatedDate() != null ? Date.valueOf(d.getCreatedDate()) : new Date(System.currentTimeMillis()));
            ps.setDate(10, d.getResolvedDate() != null ? Date.valueOf(d.getResolvedDate()) : null);
            ps.setString(11, d.getHandledByAdminId());
            ps.setString(12, d.getResolutionNote());
            ps.executeUpdate();
        }
    }

    public void update(Connection c, Dispute d) throws SQLException {
        String sql = "UPDATE disputes SET evidence_images = ?, status = ?, resolved_date = ?, "
                   + "handled_by_admin_id = ?, resolution_note = ?, "
                   + "seller_response = ?, seller_evidence_images = ?, seller_response_date = ? WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, d.getEvidenceImages());
            ps.setString(2, d.getStatus().dbValue());
            ps.setDate(3, d.getResolvedDate() != null ? Date.valueOf(d.getResolvedDate()) : null);
            ps.setString(4, d.getHandledByAdminId());
            ps.setString(5, d.getResolutionNote());
            ps.setString(6, d.getSellerResponse());
            ps.setString(7, d.getSellerEvidenceImages());
            ps.setTimestamp(8, d.getSellerResponseDate() != null ? Timestamp.valueOf(d.getSellerResponseDate()) : null);
            ps.setString(9, d.getId());
            ps.executeUpdate();
        }
    }

    /** Đơn đang có khiếu nại chưa xử lý xong (open / reviewing) không? */
    public boolean hasOpenDispute(Connection c, String orderId) throws SQLException {
        String sql = "SELECT 1 FROM disputes WHERE order_id = ? AND status IN ('open', 'reviewing')";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Dispute findByIdForUpdate(Connection c, String id) throws SQLException {
        String sql = "SELECT * FROM disputes WHERE id = ? FOR UPDATE";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public Dispute findById(Connection c, String id) throws SQLException {
        String sql = "SELECT * FROM disputes WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public Dispute findByOrderId(Connection c, String orderId) throws SQLException {
        String sql = "SELECT * FROM disputes WHERE order_id = ? ORDER BY created_date DESC LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<Dispute> findByUser(Connection c, String userId) throws SQLException {
        String sql = "SELECT * FROM disputes WHERE raised_by_id = ? OR against_id = ? ORDER BY created_date DESC";
        List<Dispute> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public List<Dispute> listPendingForAdmin(Connection c) throws SQLException {
        String sql = "SELECT * FROM disputes WHERE status IN ('open', 'reviewing') ORDER BY created_date ASC";
        List<Dispute> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public List<Dispute> listAll(Connection c, int limit) throws SQLException {
        String sql = "SELECT * FROM disputes ORDER BY created_date DESC LIMIT ?";
        List<Dispute> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    private Dispute map(ResultSet rs) throws SQLException {
        Dispute d = new Dispute();
        d.setId(rs.getString("id"));
        d.setOrderId(rs.getString("order_id"));
        d.setRaisedById(rs.getString("raised_by_id"));
        d.setAgainstId(rs.getString("against_id"));
        d.setReason(DisputeReason.fromDb(rs.getString("reason")));
        d.setDescription(rs.getString("description"));
        d.setEvidenceImages(rs.getString("evidence_images"));
        d.setStatus(DisputeStatus.fromDb(rs.getString("status")));

        Date cDate = rs.getDate("created_date");
        if (cDate != null) d.setCreatedDate(cDate.toLocalDate());

        Date rDate = rs.getDate("resolved_date");
        if (rDate != null) d.setResolvedDate(rDate.toLocalDate());

        d.setHandledByAdminId(rs.getString("handled_by_admin_id"));
        d.setResolutionNote(rs.getString("resolution_note"));
        d.setSellerResponse(rs.getString("seller_response"));
        d.setSellerEvidenceImages(rs.getString("seller_evidence_images"));
        Timestamp sDate = rs.getTimestamp("seller_response_date");
        if (sDate != null) d.setSellerResponseDate(sDate.toLocalDateTime());
        return d;
    }
}
