package org.example.cunguoimoita.admin.dao;

import org.example.cunguoimoita.admin.model.Report;
import org.example.cunguoimoita.admin.model.ReportReason;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** Bảng reports (F10). target_id trỏ products hoặc users tuỳ target_type nên lấy tên bằng LEFT JOIN cả hai. */
public class ReportDAO {

    private static final String SELECT = "SELECT r.*, ru.fullname AS reporter_name,"
            + " COALESCE(p.name, tu.fullname) AS target_name,"
            + " CASE WHEN r.target_type = 'product' THEN p.seller_id ELSE r.target_id END AS target_owner_id"
            + " FROM reports r"
            + " JOIN users ru ON ru.id = r.reporter_id"
            + " LEFT JOIN products p ON r.target_type = 'product' AND p.id = r.target_id"
            + " LEFT JOIN users tu ON r.target_type = 'user' AND tu.id = r.target_id";

    public void insert(Connection c, Report r) throws SQLException {
        String sql = "INSERT INTO reports (id, reporter_id, target_type, target_id, reason, description,"
                + " evidence_images, status, created_date) VALUES (?, ?, ?, ?, ?, ?, ?, 'pending', ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, r.getId());
            ps.setString(2, r.getReporterId());
            ps.setString(3, r.getTargetType());
            ps.setString(4, r.getTargetId());
            ps.setString(5, r.getReason().dbValue());
            ps.setString(6, r.getDescription());
            ps.setString(7, r.getEvidenceImages());
            ps.setTimestamp(8, Timestamp.valueOf(r.getCreatedDate()));
            ps.executeUpdate();
        }
    }

    /** @param status pending | resolved | rejected | null (tất cả) */
    public List<Report> find(Connection c, String status) throws SQLException {
        return find(c, status, 100);
    }

    /** @param limit số dòng tối đa (trang tổng quan chỉ lấy vài báo cáo mới nhất) */
    public List<Report> find(Connection c, String status, int limit) throws SQLException {
        boolean byStatus = status != null && !status.isBlank() && !"all".equals(status);
        String sql = SELECT + (byStatus ? " WHERE r.status = ?" : "") + " ORDER BY r.created_date DESC LIMIT " + Math.max(1, limit);
        List<Report> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            if (byStatus) {
                ps.setString(1, status);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public Report findByIdForUpdate(Connection c, String id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT + " WHERE r.id = ? FOR UPDATE OF r")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Đã có báo cáo pending của cùng người cho cùng đối tượng chưa (chống gửi trùng). */
    public boolean hasPending(Connection c, String reporterId, String targetType, String targetId) throws SQLException {
        String sql = "SELECT 1 FROM reports WHERE reporter_id = ? AND target_type = ? AND target_id = ? AND status = 'pending'";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, reporterId);
            ps.setString(2, targetType);
            ps.setString(3, targetId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Report.resolve() / bác bỏ: status = resolved | rejected, ghi admin xử lý. */
    public void close(Connection c, String reportId, String status, String adminId) throws SQLException {
        String sql = "UPDATE reports SET status = ?, handled_by_admin_id = ? WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, adminId);
            ps.setString(3, reportId);
            ps.executeUpdate();
        }
    }

    private Report map(ResultSet rs) throws SQLException {
        Report r = new Report();
        r.setId(rs.getString("id"));
        r.setReporterId(rs.getString("reporter_id"));
        r.setTargetType(rs.getString("target_type"));
        r.setTargetId(rs.getString("target_id"));
        r.setReason(ReportReason.fromDb(rs.getString("reason")));
        r.setDescription(rs.getString("description"));
        r.setEvidenceImages(rs.getString("evidence_images"));
        r.setStatus(rs.getString("status"));
        r.setCreatedDate(rs.getTimestamp("created_date").toLocalDateTime());
        r.setHandledByAdminId(rs.getString("handled_by_admin_id"));
        r.setReporterName(rs.getString("reporter_name"));
        r.setTargetName(rs.getString("target_name"));
        r.setTargetOwnerId(rs.getString("target_owner_id"));
        return r;
    }
}
